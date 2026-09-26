const fs = require('fs');
const path = require('path');

const assetsDir = path.join(__dirname, '../app/src/main/assets');
const questions = JSON.parse(fs.readFileSync(path.join(assetsDir, 'starter_questions.json'), 'utf8'));
const tests = JSON.parse(fs.readFileSync(path.join(assetsDir, 'starter_tests.json'), 'utf8'));

console.log(`=== BANK QUALITY AUDIT ===`);
console.log(`Total questions: ${questions.length}`);

const uniqueQTexts = new Set(questions.map(q => q.questionText));
console.log(`Total unique question texts: ${uniqueQTexts.size}`);

if (uniqueQTexts.size !== questions.length) {
  console.error(`FAIL: ${questions.length - uniqueQTexts.size} duplicate question texts found!`);
  process.exit(1);
}

let duplicateOptionsCount = 0;
let invalidCorrectKeyCount = 0;
const optionCounts = { a: 0, b: 0, c: 0, d: 0 };

questions.forEach(q => {
  const optTexts = q.options.map(o => o.text);
  if (new Set(optTexts).size !== 4) {
    duplicateOptionsCount++;
    console.error(`Duplicate option in ${q.id}:`, optTexts);
  }

  const correctOpt = q.options.find(o => o.id === q.correctOptionId);
  if (!correctOpt) {
    invalidCorrectKeyCount++;
    console.error(`Invalid correctOptionId ${q.correctOptionId} in ${q.id}`);
  }

  if (optionCounts[q.correctOptionId] !== undefined) {
    optionCounts[q.correctOptionId]++;
  }
});

if (duplicateOptionsCount > 0) {
  console.error(`FAIL: ${duplicateOptionsCount} questions have duplicate options!`);
  process.exit(1);
}

if (invalidCorrectKeyCount > 0) {
  console.error(`FAIL: ${invalidCorrectKeyCount} questions have invalid correctOptionId!`);
  process.exit(1);
}

console.log(`✓ 100% unique question texts (${uniqueQTexts.size}/${questions.length})`);
console.log(`✓ 100% unique options across all questions`);
console.log(`✓ Option Key Distribution (Correct Answers):`, optionCounts);

console.log(`\n=== MOCK TESTS AUDIT ===`);
const questionMap = new Map();
questions.forEach(q => questionMap.set(q.id, q));

tests.forEach(test => {
  const ids = test.questionIds;
  const texts = ids.map(id => questionMap.get(id)?.questionText).filter(Boolean);
  const uniqueTexts = new Set(texts);
  console.log(`- Test '${test.title}' (${test.id}): ${ids.length} questions, ${uniqueTexts.size}/${texts.length} unique question texts`);
  if (uniqueTexts.size !== ids.length) {
    console.error(`FAIL: Duplicate question texts in test ${test.id}`);
    process.exit(1);
  }
});

console.log(`\nALL AUDIT CHECKS PASSED SUCCESSFULLY!`);
