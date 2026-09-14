import { before, after, beforeEach, test } from 'node:test';
import { readFileSync } from 'node:fs';
import { initializeTestEnvironment, assertSucceeds, assertFails } from '@firebase/rules-unit-testing';
import { doc, setDoc, getDoc, updateDoc, arrayUnion, serverTimestamp, writeBatch, deleteDoc, Timestamp } from 'firebase/firestore';
let env;
const invite = 'apple-basil-copper-dinner-ember-forest';
const user = uid => env.authenticatedContext(uid).firestore();
const home = db => doc(db, 'households/home');
const state = db => doc(db, 'households/home/state/current');
before(async () => { env = await initializeTestEnvironment({ projectId: 'demo-pantrypal', firestore: { rules: readFileSync(new URL('firestore.rules', import.meta.url), 'utf8') } }); });
after(async () => { await env.cleanup(); });
beforeEach(async () => {
  await env.clearFirestore();
  await assertSucceeds(setDoc(home(user('owner')), { memberIds: ['owner'], inviteCode: invite, createdAt: 1 }));
});
test('outsiders cannot read household or list', async () => {
  await assertFails(getDoc(home(user('outsider'))));
  await assertFails(getDoc(state(user('outsider'))));
  await assertFails(getDoc(home(env.unauthenticatedContext().firestore())));
});
test('valid invite adds only yourself; retry is idempotent', async () => {
  await assertSucceeds(updateDoc(home(user('owner')), { memberIds: arrayUnion('owner'), 'joinProofs.owner': invite }));
  await assertFails(updateDoc(home(user('partner')), { memberIds: arrayUnion('partner') }));
  await assertFails(updateDoc(home(user('partner')), { memberIds: arrayUnion('partner'), 'joinProofs.partner.uid': 'partner' }));
  await assertSucceeds(updateDoc(home(user('partner')), { memberIds: arrayUnion('partner'), 'joinProofs.partner': invite }));
  await assertSucceeds(updateDoc(home(user('partner')), { memberIds: arrayUnion('partner'), 'joinProofs.partner': invite }));
  await assertSucceeds(getDoc(home(user('partner'))));
  await assertFails(updateDoc(home(user('third')), { memberIds: arrayUnion('third'), 'joinProofs.third': invite }));
});
test('forged invite and owner replacement fail', async () => {
  await assertFails(updateDoc(home(user('partner')), { memberIds: arrayUnion('partner') }));
  await assertFails(updateDoc(home(user('partner')), { memberIds: arrayUnion('partner'), 'joinProofs.partner': 'wrong' }));
  await assertFails(updateDoc(home(user('partner')), { memberIds: ['partner', 'intruder'], 'joinProofs.partner': invite }));
});
const author = () => ({updatedBy: 'owner', updatedAt: serverTimestamp()});
const record = db => doc(db, 'households/home/shoppingRecords/milk');
async function ready(db) {
  await setDoc(state(db), {protocol: 3, phase: 'migrating', revision: 0, ...author()});
  await setDoc(state(db), {protocol: 3, phase: 'ready', revision: 0, ...author()});
  await setDoc(state(db), {protocol: 4, phase: 'migrating', revision: 0, historyFloor: 0, ...author()});
  await setDoc(state(db), {protocol: 4, phase: 'ready', revision: 0, historyFloor: 0, ...author()});
}
test('v2 and v3 migrations preserve their seed; interrupted chunks are retryable; downgrade fails', async () => {
  const db = user('owner');
  await env.withSecurityRulesDisabled(async ctx => setDoc(state(ctx.firestore()), {protocol: 2, shoppingV2: '{"records":{}}'}));
  await assertSucceeds(updateDoc(state(db), {protocol: 3, phase: 'migrating', revision: 0, ...author()}));
  const seed = {key: 'item:milk', token: 'seed', data: '{}', deleted: false, ...author()};
  await assertSucceeds(setDoc(record(db), seed));
  await assertSucceeds(setDoc(record(db), seed));
  await assertSucceeds(setDoc(state(db), {protocol: 3, phase: 'ready', revision: 0, ...author()}));
  await assertSucceeds(setDoc(state(db), {protocol: 4, phase: 'migrating', revision: 0, historyFloor: 0, ...author()}));
  await assertSucceeds(updateDoc(record(db), {revision: 0}));
  await assertSucceeds(updateDoc(record(db), {revision: 0}));
  await assertSucceeds(setDoc(state(db), {protocol: 4, phase: 'ready', revision: 0, historyFloor: 0, ...author()}));
  await assertFails(setDoc(record(db), seed));
  await assertFails(setDoc(state(db), {protocol: 2, shoppingV2: '{}', ...author()}));
  await assertFails(updateDoc(state(db), {phase: 'migrating', ...author()}));
});
test('record writes require membership, author and an atomic revision increment', async () => {
  const db = user('owner'); await ready(db);
  const data = {key: 'item:milk', token: 'edit', data: '{}', deleted: false, revision: 1, ...author()};
  await assertFails(setDoc(record(db), data));
  const batch = writeBatch(db);
  batch.set(record(db), data);
  batch.set(doc(db, 'households/home/shoppingDevices/phone'), {batch: 'one', ...author()});
  batch.update(state(db), {revision: 1, ...author()});
  await assertSucceeds(batch.commit());
  await assertFails(getDoc(record(user('outsider'))));
  const spoof = writeBatch(db);
  spoof.set(record(db), {...data, revision: 2, updatedBy: 'partner'});
  spoof.update(state(db), {revision: 2, ...author()});
  await assertFails(spoof.commit());
});
test('only tombstones older than 30 days can be compacted', async () => {
  const db = user('owner'); await ready(db);
  await env.withSecurityRulesDisabled(async ctx => setDoc(record(ctx.firestore()), {
    key: 'item:milk', token: 'delete', data: null, deleted: true,
    revision: 0, updatedBy: 'owner', updatedAt: Timestamp.fromMillis(Date.now() - 31 * 86400000)
  }));
  const prune = writeBatch(db); prune.delete(record(db)); prune.update(state(db), {revision: 1, historyFloor: 0, ...author()});
  await assertSucceeds(prune.commit());
  const create = writeBatch(db);
  create.set(record(db), {key: 'item:milk', token: 'new', data: '{}', deleted: false, revision: 2, ...author()});
  create.update(state(db), {revision: 2, ...author()}); await create.commit();
  const bad = writeBatch(db); bad.delete(record(db)); bad.update(state(db), {revision: 3, ...author()});
  await assertFails(bad.commit());
  await assertFails(deleteDoc(record(db)));
});
test('compaction advances the retained-history floor to the deleted revision', async () => {
  const db = user('owner');
  await env.withSecurityRulesDisabled(async ctx => {
    const admin = ctx.firestore();
    await setDoc(state(admin), {
      protocol: 4, phase: 'ready', revision: 2, historyFloor: 0,
      updatedBy: 'owner', updatedAt: Timestamp.now()
    });
    await setDoc(record(admin), {
      key: 'item:milk', token: 'delete', data: null, deleted: true, revision: 2,
      updatedBy: 'owner', updatedAt: Timestamp.fromMillis(Date.now() - 31 * 86400000)
    });
  });
  const unsafe = writeBatch(db);
  unsafe.delete(record(db));
  unsafe.update(state(db), {revision: 3, ...author()});
  await assertFails(unsafe.commit());

  const safe = writeBatch(db);
  safe.delete(record(db));
  safe.update(state(db), {revision: 3, historyFloor: 2, ...author()});
  await assertSucceeds(safe.commit());
});
