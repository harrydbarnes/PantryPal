import { before, after, beforeEach, test } from 'node:test';
import { readFileSync } from 'node:fs';
import { initializeTestEnvironment, assertSucceeds, assertFails } from '@firebase/rules-unit-testing';
import { doc, setDoc, getDoc, updateDoc, arrayUnion, serverTimestamp } from 'firebase/firestore';
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
  await assertSucceeds(updateDoc(home(user('owner')), { memberIds: arrayUnion('owner'), joinProof: { code: invite, uid: 'owner' } }));
  await assertFails(updateDoc(home(user('partner')), { memberIds: arrayUnion('partner') }));
  await assertSucceeds(updateDoc(home(user('partner')), { memberIds: arrayUnion('partner'), joinProof: { code: invite, uid: 'partner' } }));
  await assertSucceeds(updateDoc(home(user('partner')), { memberIds: arrayUnion('partner'), joinProof: { code: invite, uid: 'partner' } }));
  await assertSucceeds(getDoc(home(user('partner'))));
  await assertFails(updateDoc(home(user('third')), { memberIds: arrayUnion('third'), joinProof: { code: invite, uid: 'third' } }));
});
test('forged invite and owner replacement fail', async () => {
  await assertFails(updateDoc(home(user('partner')), { memberIds: arrayUnion('partner') }));
  await assertFails(updateDoc(home(user('partner')), { memberIds: arrayUnion('partner'), joinProof: { code: 'wrong', uid: 'partner' } }));
  await assertFails(updateDoc(home(user('partner')), { memberIds: ['partner', 'intruder'], joinProof: { code: invite, uid: 'partner' } }));
});
test('members can publish v2; old snapshots and spoofed authors cannot', async () => {
  await assertSucceeds(setDoc(state(user('owner')), { protocol: 2, shoppingV2: '{}', updatedBy: 'owner', updatedAt: serverTimestamp() }));
  await assertFails(setDoc(state(user('owner')), { snapshot: 'legacy', updatedBy: 'owner', updatedAt: 1 }));
  await assertFails(setDoc(state(user('owner')), { protocol: 2, shoppingV2: '{}', updatedBy: 'other', updatedAt: serverTimestamp() }));
  await assertFails(setDoc(state(user('outsider')), { protocol: 2, shoppingV2: '{}', updatedBy: 'outsider', updatedAt: serverTimestamp() }));
});
