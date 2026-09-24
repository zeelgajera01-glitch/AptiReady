/**
 * AptiRise Content Import Tool
 *
 * Usage:
 *   # Dry-run validation (Default / Read-only):
 *   node scripts/import_content.js --project=aptiready --dry-run
 *
 *   # Import to local Firestore Emulator (port 8080):
 *   node scripts/import_content.js --emulator --apply
 *
 *   # Apply import to live Firebase project (requires explicit --project and --apply):
 *   node scripts/import_content.js --project=aptiready --apply
 */

const fs = require('fs');
const path = require('path');

const args = process.argv.slice(2);
const hasApply = args.includes('--apply');
const hasDryRun = args.includes('--dry-run');
const isEmulator = args.includes('--emulator');

// Rule: Refuse conflicting --dry-run and --apply flags
if (hasApply && hasDryRun) {
  console.error('ERROR: Cannot specify both --dry-run and --apply flags.');
  process.exit(1);
}

const isDryRun = !hasApply || hasDryRun;

// Rule: Check FIRESTORE_EMULATOR_HOST environment variable
if (process.env.FIRESTORE_EMULATOR_HOST && !isEmulator) {
  console.error('ERROR: FIRESTORE_EMULATOR_HOST is set in environment, but --emulator flag was not passed.');
  console.error('Aborting to avoid unintended emulator routing.');
  process.exit(1);
}

// Extract --project=<id>
const projectArg = args.find(a => a.startsWith('--project='));
const projectIdInput = projectArg ? projectArg.split('=')[1].trim() : '';

// Rule: For a live write, require both --apply and an explicit nonempty --project argument
if (!isDryRun && !isEmulator && !projectIdInput) {
  console.error('ERROR: Live import (--apply) requires an explicit --project=<id> parameter (e.g. --project=aptiready).');
  console.error('Fallback project IDs are not permitted for live writes.');
  process.exit(1);
}

const projectId = projectIdInput || 'aptiready';

console.log('=== AptiRise Content Import Tool ===');
console.log(`Mode: ${isDryRun ? 'DRY-RUN (Validation only, no database writes)' : 'APPLY (Writing to Firestore)'}`);
console.log(`Target Project: ${projectId}`);
console.log(`Database Target: (default)`);
console.log(`Emulator Mode: ${isEmulator ? 'ENABLED (127.0.0.1:8080)' : 'DISABLED'}`);

// Paths to starter JSON files
const assetsDir = path.join(__dirname, '../app/src/main/assets');
const categoriesPath = path.join(assetsDir, 'starter_categories.json');
const topicsPath = path.join(assetsDir, 'starter_topics.json');
const questionsPath = path.join(assetsDir, 'starter_questions.json');
const testsPath = path.join(assetsDir, 'starter_tests.json');

function loadJsonFile(filePath, label) {
  if (!fs.existsSync(filePath)) {
    console.error(`ERROR: ${label} file not found at ${filePath}`);
    process.exit(1);
  }
  try {
    const raw = fs.readFileSync(filePath, 'utf8');
    const data = JSON.parse(raw);
    if (!Array.isArray(data)) {
      console.error(`ERROR: ${label} JSON root must be an array.`);
      process.exit(1);
    }
    return data;
  } catch (e) {
    console.error(`ERROR: Failed to parse ${label} JSON (${filePath}):`, e.message);
    process.exit(1);
  }
}

const categories = loadJsonFile(categoriesPath, 'starter_categories.json');
const topics = loadJsonFile(topicsPath, 'starter_topics.json');
const questions = loadJsonFile(questionsPath, 'starter_questions.json');
const tests = loadJsonFile(testsPath, 'starter_tests.json');

console.log(`\nLoaded Source Data:`);
console.log(`- Categories: ${categories.length}`);
console.log(`- Topics: ${topics.length}`);
console.log(`- Practice Questions: ${questions.length}`);
console.log(`- Mock Tests: ${tests.length}`);
console.log(`- Learning Notes: Embedded in 'topics' collection under 'formulaPreview' (no separate learningNotes collection implemented).`);

let errors = 0;
const validCategoryIds = new Set();
const validTopicIds = new Set();
const validQuestionIds = new Set();
const validDifficulties = new Set(['easy', 'medium', 'hard']);

// 1. Validate Categories
categories.forEach((c, idx) => {
  if (!c.id || typeof c.id !== 'string' || c.id.trim() === '') { console.error(`[Category#${idx}] Invalid or missing 'id'`); errors++; }
  if (validCategoryIds.has(c.id)) { console.error(`[Category#${idx}] Duplicate Category ID '${c.id}'`); errors++; }
  validCategoryIds.add(c.id);

  if (!c.title || c.title.trim() === '') { console.error(`[Category#${idx} - ${c.id}] Empty 'title'`); errors++; }
  if (!c.description || c.description.trim() === '') { console.error(`[Category#${idx} - ${c.id}] Empty 'description'`); errors++; }
  if (typeof c.sortOrder !== 'number') { console.error(`[Category#${idx} - ${c.id}] Invalid 'sortOrder'`); errors++; }
  if (c.published !== true) { console.error(`[Category#${idx} - ${c.id}] Starter category must have 'published: true'`); errors++; }
});

// 2. Validate Topics
topics.forEach((t, idx) => {
  if (!t.id || typeof t.id !== 'string' || t.id.trim() === '') { console.error(`[Topic#${idx}] Invalid or missing 'id'`); errors++; }
  if (validTopicIds.has(t.id)) { console.error(`[Topic#${idx}] Duplicate Topic ID '${t.id}'`); errors++; }
  validTopicIds.add(t.id);

  if (!t.categoryId || !validCategoryIds.has(t.categoryId)) {
    console.error(`[Topic#${idx} - ${t.id}] Referenced categoryId '${t.categoryId}' not found in categories`);
    errors++;
  }
  if (!t.categoryName || t.categoryName.trim() === '') { console.error(`[Topic#${idx} - ${t.id}] Empty 'categoryName'`); errors++; }
  if (!t.title || t.title.trim() === '') { console.error(`[Topic#${idx} - ${t.id}] Empty 'title'`); errors++; }
  if (!t.description || t.description.trim() === '') { console.error(`[Topic#${idx} - ${t.id}] Empty 'description'`); errors++; }
  if (!validDifficulties.has(t.difficulty)) { console.error(`[Topic#${idx} - ${t.id}] Invalid 'difficulty' '${t.difficulty}'`); errors++; }
  if (typeof t.sampleQuestionCount !== 'number' || t.sampleQuestionCount <= 0) { console.error(`[Topic#${idx} - ${t.id}] Invalid 'sampleQuestionCount'`); errors++; }
  if (!t.formulaPreview || t.formulaPreview.trim() === '') { console.error(`[Topic#${idx} - ${t.id}] Empty 'formulaPreview'`); errors++; }
  if (typeof t.sortOrder !== 'number') { console.error(`[Topic#${idx} - ${t.id}] Invalid 'sortOrder'`); errors++; }
  if (t.published !== true) { console.error(`[Topic#${idx} - ${t.id}] Starter topic must have 'published: true'`); errors++; }
});

// 3. Validate Questions
questions.forEach((q, idx) => {
  if (!q.id || typeof q.id !== 'string' || q.id.trim() === '') { console.error(`[Question#${idx}] Invalid or missing 'id'`); errors++; }
  if (validQuestionIds.has(q.id)) { console.error(`[Question#${idx}] Duplicate Question ID '${q.id}'`); errors++; }
  validQuestionIds.add(q.id);

  if (!q.topicId || !validTopicIds.has(q.topicId)) {
    console.error(`[Question#${idx} - ${q.id}] Referenced topicId '${q.topicId}' not found in topics`);
    errors++;
  }
  if (!q.categoryId || !validCategoryIds.has(q.categoryId)) {
    console.error(`[Question#${idx} - ${q.id}] Referenced categoryId '${q.categoryId}' not found in categories`);
    errors++;
  }
  if (!validDifficulties.has(q.difficulty)) { console.error(`[Question#${idx} - ${q.id}] Invalid 'difficulty' '${q.difficulty}'`); errors++; }
  if (!q.languageCode || q.languageCode.trim() === '') { console.error(`[Question#${idx} - ${q.id}] Empty 'languageCode'`); errors++; }
  if (typeof q.version !== 'number') { console.error(`[Question#${idx} - ${q.id}] Invalid 'version'`); errors++; }
  if (!q.questionText || q.questionText.trim() === '') { console.error(`[Question#${idx} - ${q.id}] Empty 'questionText'`); errors++; }
  if (!Array.isArray(q.options) || q.options.length !== 4) { console.error(`[Question#${idx} - ${q.id}] 'options' must be an array of exactly 4 items`); errors++; }

  const optionIds = new Set();
  (q.options || []).forEach((opt, oIdx) => {
    if (!opt.id || typeof opt.id !== 'string' || opt.id.trim() === '') { console.error(`[Question#${idx} - ${q.id}] Option #${oIdx} missing 'id'`); errors++; }
    if (optionIds.has(opt.id)) { console.error(`[Question#${idx} - ${q.id}] Duplicate Option ID '${opt.id}'`); errors++; }
    optionIds.add(opt.id);
    if (!opt.text || opt.text.trim() === '') { console.error(`[Question#${idx} - ${q.id}] Option #${oIdx} empty 'text'`); errors++; }
  });

  if (!optionIds.has(q.correctOptionId)) { console.error(`[Question#${idx} - ${q.id}] 'correctOptionId' '${q.correctOptionId}' not found in options`); errors++; }
  if (!q.explanation || q.explanation.trim() === '') { console.error(`[Question#${idx} - ${q.id}] Empty 'explanation'`); errors++; }
  if (!q.hint || q.hint.trim() === '') { console.error(`[Question#${idx} - ${q.id}] Empty 'hint'`); errors++; }
  if (q.published !== true) { console.error(`[Question#${idx} - ${q.id}] Starter question must have 'published: true'`); errors++; }
});

// 4. Validate Mock Tests
const seenTestIds = new Set();
tests.forEach((t, idx) => {
  if (!t.id || typeof t.id !== 'string' || t.id.trim() === '') { console.error(`[MockTest#${idx}] Invalid or missing 'id'`); errors++; }
  if (seenTestIds.has(t.id)) { console.error(`[MockTest#${idx}] Duplicate Test ID '${t.id}'`); errors++; }
  seenTestIds.add(t.id);

  if (!t.title || t.title.trim() === '') { console.error(`[MockTest#${idx} - ${t.id}] Empty 'title'`); errors++; }
  if (!t.description || t.description.trim() === '') { console.error(`[MockTest#${idx} - ${t.id}] Empty 'description'`); errors++; }
  if (!Array.isArray(t.questionIds) || t.questionIds.length === 0) { console.error(`[MockTest#${idx} - ${t.id}] Empty or missing 'questionIds' array`); errors++; }
  if (typeof t.durationSeconds !== 'number' || t.durationSeconds <= 0 || !Number.isInteger(t.durationSeconds)) {
    console.error(`[MockTest#${idx} - ${t.id}] 'durationSeconds' must be a positive integer`);
    errors++;
  }
  if (typeof t.marksPerCorrect !== 'number' || t.marksPerCorrect <= 0 || !Number.isInteger(t.marksPerCorrect)) {
    console.error(`[MockTest#${idx} - ${t.id}] 'marksPerCorrect' must be a positive integer`);
    errors++;
  }
  if (t.published !== true) { console.error(`[MockTest#${idx} - ${t.id}] Starter mock test must have 'published: true'`); errors++; }

  (t.questionIds || []).forEach(qId => {
    if (!validQuestionIds.has(qId)) {
      console.error(`[MockTest#${idx} - ${t.id}] Referenced question ID '${qId}' not found in practiceQuestions bank`);
      errors++;
    }
  });
});

if (errors > 0) {
  console.error(`\nValidation FAILED with ${errors} error(s). Aborting.`);
  process.exit(1);
}

console.log(`\nValidation PASSED successfully for all records!`);
console.log(`- Categories: ${categories.length} valid`);
console.log(`- Topics: ${topics.length} valid`);
console.log(`- Practice Questions: ${questions.length} valid`);
console.log(`- Mock Tests: ${tests.length} valid`);

if (isDryRun) {
  console.log('\nDRY-RUN Execution Complete.');
  console.log('No Admin SDK initialized and zero writes performed to Firestore.');
  process.exit(0);
}

// Live or Emulator Write Phase
if (isEmulator) {
  process.env.FIRESTORE_EMULATOR_HOST = '127.0.0.1:8080';
}

const {
  initializeApp,
  applicationDefault
} = require('firebase-admin/app');

const {
  getFirestore,
  FieldValue
} = require('firebase-admin/firestore');

const app = initializeApp({
  projectId,
  ...(isEmulator ? {} : { credential: applicationDefault() })
});

const db = getFirestore(app);

async function performImport() {
  console.log(`\nBeginning Firestore chunked batch write to project '${projectId}'...`);

  const allOps = [];

  categories.forEach(c => {
    allOps.push({ ref: db.collection('categories').doc(c.id), data: { ...c, updatedAt: FieldValue.serverTimestamp() } });
  });

  topics.forEach(t => {
    allOps.push({ ref: db.collection('topics').doc(t.id), data: { ...t, updatedAt: FieldValue.serverTimestamp() } });
  });

  questions.forEach(q => {
    allOps.push({ ref: db.collection('practiceQuestions').doc(q.id), data: { ...q, updatedAt: FieldValue.serverTimestamp() } });
  });

  tests.forEach(t => {
    allOps.push({ ref: db.collection('mockTests').doc(t.id), data: { ...t, updatedAt: FieldValue.serverTimestamp() } });
  });

  const BATCH_SIZE = 300;
  let committedCount = 0;
  let batchIndex = 0;

  for (let i = 0; i < allOps.length; i += BATCH_SIZE) {
    batchIndex++;
    const chunk = allOps.slice(i, i + BATCH_SIZE);
    const batch = db.batch();

    chunk.forEach(op => {
      batch.set(op.ref, op.data, { merge: true });
    });

    await batch.commit();
    committedCount += chunk.length;
    console.log(`- Batch #${batchIndex} committed successfully (${chunk.length} items, total committed: ${committedCount}/${allOps.length})`);
  }

  console.log(`\nSUCCESS: Successfully committed ${committedCount} total documents across ${batchIndex} batch(es) to Firestore project '${projectId}'!`);
  console.log(`- 'categories': ${categories.length} docs`);
  console.log(`- 'topics': ${topics.length} docs`);
  console.log(`- 'practiceQuestions': ${questions.length} docs`);
  console.log(`- 'mockTests': ${tests.length} docs`);
}

performImport().catch(err => {
  console.error('ERROR during Firestore import:', err);
  process.exit(1);
});