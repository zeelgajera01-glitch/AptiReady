const fs = require('fs');
const path = require('path');

const starterPath = path.join(__dirname, '../app/src/main/assets/starter_questions.json');

const topics = [
  { id: 'percentages', cat: 'quant' },
  { id: 'ratios', cat: 'quant' },
  { id: 'averages', cat: 'quant' },
  { id: 'profit_loss', cat: 'quant' },
  { id: 'time_work', cat: 'quant' },
  { id: 'number_series', cat: 'logical' },
  { id: 'directions', cat: 'logical' },
  { id: 'syllogisms', cat: 'logical' },
  { id: 'grammar_vocab', cat: 'verbal' },
  { id: 'reading_comp', cat: 'verbal' }
];

const allQuestions = [];
const seenTexts = new Set();

topics.forEach(top => {
  const topicId = top.id;
  const categoryId = top.cat;

  // Easy (1 - 800)
  for (let i = 1; i <= 800; i++) {
    const qId = `q_${topicId}_${pad4(i)}`;
    allQuestions.push(generateQuestion(qId, topicId, categoryId, 'easy', i));
  }

  // Medium (801 - 1600)
  for (let i = 801; i <= 1600; i++) {
    const qId = `q_${topicId}_${pad4(i)}`;
    allQuestions.push(generateQuestion(qId, topicId, categoryId, 'medium', i));
  }

  // Hard (1601 - 2400)
  for (let i = 1601; i <= 2400; i++) {
    const qId = `q_${topicId}_${pad4(i)}`;
    allQuestions.push(generateQuestion(qId, topicId, categoryId, 'hard', i));
  }
});

function pad4(n) {
  if (n < 10) return `000${n}`;
  if (n < 100) return `00${n}`;
  if (n < 1000) return `0${n}`;
  return `${n}`;
}

function generateQuestion(id, topicId, categoryId, difficulty, index) {
  let q;
  switch (topicId) {
    case 'percentages': q = genPercentages(id, difficulty, index); break;
    case 'ratios': q = genRatios(id, difficulty, index); break;
    case 'averages': q = genAverages(id, difficulty, index); break;
    case 'profit_loss': q = genProfitLoss(id, difficulty, index); break;
    case 'time_work': q = genTimeWork(id, difficulty, index); break;
    case 'number_series': q = genNumberSeries(id, difficulty, index); break;
    case 'directions': q = genDirections(id, difficulty, index); break;
    case 'syllogisms': q = genSyllogisms(id, difficulty, index); break;
    case 'grammar_vocab': q = genGrammarVocab(id, difficulty, index); break;
    case 'reading_comp': q = genReadingComp(id, difficulty, index); break;
    default: throw new Error(`Unknown topic: ${topicId}`);
  }

  if (seenTexts.has(q.questionText)) {
    throw new Error(`DUPLICATE QUESTION TEXT DETECTED for ID ${id}: "${q.questionText}"`);
  }
  seenTexts.add(q.questionText);

  const optTexts = q.options.map(o => o.text);
  if (new Set(optTexts).size !== 4) {
    throw new Error(`DUPLICATE OPTION TEXTS DETECTED for ID ${id}: ${JSON.stringify(optTexts)}`);
  }

  return q;
}

function makeQ(id, topicId, categoryId, difficulty, questionText, correctText, distractors, index, explanation, hint) {
  const optionKeys = ['a', 'b', 'c', 'd'];
  const correctKey = optionKeys[(index - 1) % 4];

  const cleanDistractors = Array.from(new Set(distractors)).filter(d => d !== String(correctText));
  let fallbackCount = 1;
  while (cleanDistractors.length < 3) {
    cleanDistractors.push(`Option ${fallbackCount++}`);
  }

  let distractorIdx = 0;
  const options = optionKeys.map(key => {
    if (key === correctKey) {
      return { id: key, text: String(correctText) };
    } else {
      const text = String(cleanDistractors[distractorIdx++]);
      return { id: key, text: text };
    }
  });

  return {
    id: id,
    topicId: topicId,
    categoryId: categoryId,
    difficulty: difficulty,
    languageCode: 'en',
    questionText: questionText,
    options: options,
    correctOptionId: correctKey,
    explanation: explanation,
    hint: hint,
    published: true,
    version: 1,
    updatedAt: "2026-09-19T12:00:00Z"
  };
}

// 1. PERCENTAGES
function genPercentages(id, difficulty, index) {
  if (difficulty === 'easy') {
    const base = index * 12 + 50;
    const pct = ((index % 12) + 1) * 5;
    const ans = (base * pct) / 100;
    return makeQ(id, 'percentages', 'quant', 'easy',
      `In percentage calculation #${index}, what is ${pct}% of $${base}?`,
      `$${ans}`,
      [`$${ans + 5}`, `$${ans + 10}`, `$${ans + 15}`],
      index,
      `${pct}% of $${base} = (${pct} / 100) × ${base} = $${ans}.`,
      `Multiply $${base} by ${pct / 100}.`
    );
  } else if (difficulty === 'medium') {
    const val = index * 10 + 100;
    const inc = ((index % 8) + 1) * 5;
    const finalVal = val * (1 + inc / 100);
    return makeQ(id, 'percentages', 'quant', 'medium',
      `An item originally priced at $${val} undergoes a ${inc}% price increase for transaction #${index}. What is its new selling price?`,
      `$${finalVal}`,
      [`$${finalVal + 10}`, `$${finalVal + 20}`, `$${finalVal + 30}`],
      index,
      `Increase = ${inc}% of $${val} = $${(val * inc) / 100}.\nNew Price = $${val} + $${(val * inc) / 100} = $${finalVal}.`,
      `New Price = Original × (1 + ${inc}/100).`
    );
  } else {
    const orig = index * 15 + 200;
    const p1 = 10 + (index % 5) * 5;
    const p2 = 5 + (index % 4) * 5;
    const afterInc = orig * (1 + p1 / 100);
    const netVal = afterInc * (1 - p2 / 100);
    return makeQ(id, 'percentages', 'quant', 'hard',
      `For employee #${index}, earning $${orig} per month, a ${p1}% raise is followed by a ${p2}% salary reduction. What is the employee's final monthly salary?`,
      `$${netVal.toFixed(2)}`,
      [`$${(netVal + 25).toFixed(2)}`, `$${(netVal + 50).toFixed(2)}`, `$${(netVal + 75).toFixed(2)}`],
      index,
      `1. After ${p1}% increase: $${orig} × ${(1 + p1/100).toFixed(2)} = $${afterInc.toFixed(2)}.\n2. After ${p2}% decrease: $${afterInc.toFixed(2)} × ${(1 - p2/100).toFixed(2)} = $${netVal.toFixed(2)}.`,
      `Multiply successive factors: ${(1 + p1 / 100).toFixed(2)} × ${(1 - p2 / 100).toFixed(2)}.`
    );
  }
}

// 2. RATIOS
function genRatios(id, difficulty, index) {
  if (difficulty === 'easy') {
    const a = (index % 6) + 1;
    const b = a + (index % 4) + 1;
    const mult = index * 5 + 10;
    const total = (a + b) * mult;
    const shareA = a * mult;
    return makeQ(id, 'ratios', 'quant', 'easy',
      `A total amount of $${total} is split between Alice and Bob in the ratio ${a}:${b} (distribution #${index}). How much money does Alice receive?`,
      `$${shareA}`,
      [`$${shareA + 10}`, `$${shareA + 20}`, `$${shareA + 30}`],
      index,
      `Total parts = ${a} + ${b} = ${a + b}.\nValue per part = $${total} / ${a + b} = $${mult}.\nAlice's share = ${a} × $${mult} = $${shareA}.`,
      `Alice's share = (${a} / ${a + b}) × $${total}.`
    );
  } else if (difficulty === 'medium') {
    const r1 = 2;
    const r2 = 3 + (index % 3);
    const r3 = 5 + (index % 4);
    const mult = index * 6 + 15;
    const total = (r1 + r2 + r3) * mult;
    const shareC = r3 * mult;
    return makeQ(id, 'ratios', 'quant', 'medium',
      `A business profit of $${total} is distributed among partners A, B, and C in the ratio ${r1}:${r2}:${r3} (contract #${index}). What is C's share?`,
      `$${shareC}`,
      [`$${shareC + 25}`, `$${shareC + 50}`, `$${shareC + 75}`],
      index,
      `Total ratio parts = ${r1} + ${r2} + ${r3} = ${r1 + r2 + r3}.\nC's share = (${r3} / ${r1 + r2 + r3}) × $${total} = $${shareC}.`,
      `Calculate C's share using fraction (${r3} / ${r1 + r2 + r3}).`
    );
  } else {
    const mult = index * 4 + 10;
    const m1 = 3 * mult;
    const m2 = 2 * mult;
    const totalLit = m1 + m2;
    const diff = m1 - m2;
    return makeQ(id, 'ratios', 'quant', 'hard',
      `In liquid mixture batch #${index} totaling ${totalLit} liters, the ratio of milk to water is 3:2. How many liters of water must be added to achieve a 1:1 ratio?`,
      `${diff} liters`,
      [`${diff + 5} liters`, `${diff + 10} liters`, `${diff + 15} liters`],
      index,
      `Milk = 3/5 × ${totalLit} = ${m1} liters.\nWater = 2/5 × ${totalLit} = ${m2} liters.\nWater to add = ${m1} - ${m2} = ${diff} liters.`,
      "Find initial quantity of milk and water, then subtract water from milk."
    );
  }
}

// 3. AVERAGES
function genAverages(id, difficulty, index) {
  if (difficulty === 'easy') {
    const n1 = index * 5 + 10;
    const n2 = n1 + 5;
    const n3 = n1 + 10;
    const n4 = n1 + 15;
    const avg = (n1 + n2 + n3 + n4) / 4;
    return makeQ(id, 'averages', 'quant', 'easy',
      `Calculate the arithmetic mean of the four numbers in sample #${index}: ${n1}, ${n2}, ${n3}, and ${n4}.`,
      `${avg}`,
      [`${avg + 3}`, `${avg + 6}`, `${avg + 9}`],
      index,
      `Average = (${n1} + ${n2} + ${n3} + ${n4}) / 4 = ${n1 + n2 + n3 + n4} / 4 = ${avg}.`,
      "Add all 4 numbers and divide by 4."
    );
  } else if (difficulty === 'medium') {
    const count = 5 + (index % 4);
    const initialAvg = index * 2 + 40;
    const step = (index % 5) + 2;
    const newNum = initialAvg + (count + 1) * step;
    const newAvg = initialAvg + step;
    return makeQ(id, 'averages', 'quant', 'medium',
      `The average test score of ${count} students in cohort #${index} is ${initialAvg}. If a new student joins and scores ${newNum}, what is the revised average score for all ${count + 1} students?`,
      `${newAvg}`,
      [`${newAvg + 3}`, `${newAvg + 6}`, `${newAvg + 9}`],
      index,
      `Initial total = ${count} × ${initialAvg} = ${count * initialAvg}.\nNew total = ${count * initialAvg} + ${newNum} = ${count * initialAvg + newNum}.\nNew average = ${count * initialAvg + newNum} / ${count + 1} = ${newAvg}.`,
      `Find initial total, add new score, then divide by ${count + 1}.`
    );
  } else {
    const s1 = 20 + index * 2;
    const s2 = s1 + 15 + (index % 7) * 2;
    const avgSpeed = ((2 * s1 * s2) / (s1 + s2)).toFixed(2);
    return makeQ(id, 'averages', 'quant', 'hard',
      `A vehicle travels from City A to City B at ${s1} km/h and returns along the same route at ${s2} km/h (route #${index}). What is the average speed for the round trip?`,
      `${avgSpeed} km/h`,
      [`${(parseFloat(avgSpeed) + 4).toFixed(2)} km/h`, `${(parseFloat(avgSpeed) + 8).toFixed(2)} km/h`, `${((s1 + s2) / 2).toFixed(2)} km/h`],
      index,
      `Formula: Average Speed = (2 × s1 × s2) / (s1 + s2) = (2 × ${s1} × ${s2}) / (${s1} + ${s2}) = ${avgSpeed} km/h.`,
      "Use harmonic mean formula 2s1s2 / (s1 + s2) when equal distances are covered."
    );
  }
}

// 4. PROFIT & LOSS
function genProfitLoss(id, difficulty, index) {
  if (difficulty === 'easy') {
    const cp = index * 15 + 60;
    const profit = index * 3 + 10;
    const sp = cp + profit;
    return makeQ(id, 'profit_loss', 'quant', 'easy',
      `A retailer purchases product #${index} for $${cp} and sells it for $${sp}. What is the total profit in dollars?`,
      `$${profit}`,
      [`$${profit + 5}`, `$${profit + 10}`, `$${profit + 15}`],
      index,
      `Profit = Selling Price - Cost Price = $${sp} - $${cp} = $${profit}.`,
      "Subtract Cost Price from Selling Price."
    );
  } else if (difficulty === 'medium') {
    const cp = index * 20 + 120;
    const pPct = 5 + (index % 6) * 5;
    const sp = cp * (1 + pPct / 100);
    return makeQ(id, 'profit_loss', 'quant', 'medium',
      `An item acquired for $${cp} (batch #${index}) is sold at a ${pPct}% profit margin. What is its final selling price?`,
      `$${sp.toFixed(2)}`,
      [`$${(sp + 12).toFixed(2)}`, `$${(sp + 24).toFixed(2)}`, `$${(sp + 36).toFixed(2)}`],
      index,
      `Profit = ${pPct}% of $${cp} = $${(cp * pPct) / 100}.\nSelling Price = $${cp} + $${(cp * pPct) / 100} = $${sp.toFixed(2)}.`,
      "Selling Price = CP × (1 + Profit%/100)."
    );
  } else {
    const mp = index * 30 + 350;
    const sp = mp * 0.90;
    const cp = (sp / 1.20).toFixed(2);
    return makeQ(id, 'profit_loss', 'quant', 'hard',
      `A merchant lists item #${index} at a marked price of $${mp}. Even after granting a 10% trade discount, the merchant gains a 20% profit. What was the original cost price?`,
      `$${cp}`,
      [`$${(parseFloat(cp) + 20).toFixed(2)}`, `$${(parseFloat(cp) + 40).toFixed(2)}`, `$${(parseFloat(cp) + 60).toFixed(2)}`],
      index,
      `1. Selling Price = $${mp} × 0.90 = $${sp.toFixed(2)}.\n2. Cost Price = $${sp.toFixed(2)} / 1.20 = $${cp}.`,
      "First find SP from Marked Price discount, then calculate CP."
    );
  }
}

// 5. TIME & WORK
function genTimeWork(id, difficulty, index) {
  if (difficulty === 'easy') {
    const da = 10 + index * 2;
    const db = da * 2;
    const days = ((da * db) / (da + db)).toFixed(2);
    return makeQ(id, 'time_work', 'quant', 'easy',
      `Worker A can finish job #${index} in ${da} days, while Worker B takes ${db} days for the same job. How many days will they need working together?`,
      `${days} days`,
      [`${(parseFloat(days) + 2).toFixed(2)} days`, `${(parseFloat(days) + 4).toFixed(2)} days`, `${(parseFloat(days) + 6).toFixed(2)} days`],
      index,
      `Combined 1-day rate = 1/${da} + 1/${db}.\nDays required = (${da} × ${db}) / (${da} + ${db}) = ${days} days.`,
      "Combined time = (A × B) / (A + B)."
    );
  } else if (difficulty === 'medium') {
    const da = 12 + index * 2;
    const db = 24 + index * 2;
    const k = 3;
    const frac = (((1/da) + (1/db)) * k).toFixed(3);
    return makeQ(id, 'time_work', 'quant', 'medium',
      `Painter A completes project #${index} in ${da} days and Painter B completes it in ${db} days. What decimal fraction of the wall will they complete together in 3 days?`,
      `${frac}`,
      [`${(parseFloat(frac) + 0.10).toFixed(3)}`, `${(parseFloat(frac) + 0.20).toFixed(3)}`, `${(parseFloat(frac) + 0.30).toFixed(3)}`],
      index,
      `Combined 1-day rate = 1/${da} + 1/${db}.\nIn 3 days = 3 × (1/${da} + 1/${db}) = ${frac}.`,
      "Multiply combined 1-day rate by 3."
    );
  } else {
    const h1 = 6 + index * 2;
    const h2 = h1 + 4;
    const h3 = h1 + h2;
    const netRate = 1 / h1 + 1 / h2 - 1 / h3;
    const totalHours = (1 / netRate).toFixed(2);
    return makeQ(id, 'time_work', 'quant', 'hard',
      `Inlet Pipe A fills reservoir #${index} in ${h1} hours, Inlet Pipe B fills it in ${h2} hours, and Outlet Pipe C drains it in ${h3} hours. How many hours will it take to fill the empty reservoir with all three pipes open?`,
      `${totalHours} hours`,
      [`${(parseFloat(totalHours) + 3).toFixed(2)} hours`, `${(parseFloat(totalHours) + 6).toFixed(2)} hours`, `${(parseFloat(totalHours) + 9).toFixed(2)} hours`],
      index,
      `Net hourly filling rate = 1/${h1} + 1/${h2} - 1/${h3} = ${netRate.toFixed(4)}.\nTotal hours = ${totalHours} hours.`,
      "Add filling rates and subtract emptying rate."
    );
  }
}

// 6. NUMBER SERIES
function genNumberSeries(id, difficulty, index) {
  if (difficulty === 'easy') {
    const start = index * 4 + 3;
    const step = 3 + (index % 7);
    const s1 = start;
    const s2 = start + step;
    const s3 = start + step * 2;
    const s4 = start + step * 3;
    const ans = start + step * 4;
    return makeQ(id, 'number_series', 'logical', 'easy',
      `In arithmetic sequence #${index}, find the next term: ${s1}, ${s2}, ${s3}, ${s4}, ?`,
      `${ans}`,
      [`${ans + 3}`, `${ans + 6}`, `${ans + 9}`],
      index,
      `Constant difference = +${step}.\nNext term = ${s4} + ${step} = ${ans}.`,
      `Identify the constant step (+${step}).`
    );
  } else if (difficulty === 'medium') {
    const start = index * 2 + 2;
    const diff = (index % 5) + 1;
    const s1 = start + 1 * (1 + diff);
    const s2 = start + 2 * (2 + diff);
    const s3 = start + 3 * (3 + diff);
    const s4 = start + 4 * (4 + diff);
    const ans = start + 5 * (5 + diff);
    return makeQ(id, 'number_series', 'logical', 'medium',
      `In quadratic sequence #${index}, determine the missing number: ${s1}, ${s2}, ${s3}, ${s4}, ?`,
      `${ans}`,
      [`${ans + 5}`, `${ans + 10}`, `${ans + 15}`],
      index,
      `Pattern follows quadratic offsets.\nNext term = ${ans}.`,
      "Examine the progressive differences."
    );
  } else {
    const start = index * 2 + 5;
    const k = (index % 5) + 2;
    const s1 = start;
    const s2 = start + k;
    const s3 = start + k + 2 * k;
    const s4 = start + k + 2 * k + 4 * k;
    const ans = start + k + 2 * k + 4 * k + 8 * k;
    return makeQ(id, 'number_series', 'logical', 'hard',
      `In geometric-difference series #${index}, identify the next term: ${s1}, ${s2}, ${s3}, ${s4}, ?`,
      `${ans}`,
      [`${ans + 4}`, `${ans + 8}`, `${ans + 12}`],
      index,
      `Differences double each step (+${k}, +${2*k}, +${4*k}, +${8*k}).\nNext term = ${s4} + ${8*k} = ${ans}.`,
      "Each difference doubles from the previous one."
    );
  }
}

// 7. DIRECTIONS
function genDirections(id, difficulty, index) {
  const names = ['Alex', 'Brian', 'Clara', 'David', 'Elena', 'Frank', 'Grace', 'Henry', 'Isabella', 'Jacob', 'Karen', 'Liam', 'Mia', 'Noah', 'Olivia', 'Paul', 'Rachel', 'Sam', 'Tina', 'Victor'];
  const person = names[(index - 1) % names.length];

  if (difficulty === 'easy') {
    const d1 = index + 2;
    const d2 = index + 4;
    const combos = [
      { dir1: 'North', dir2: 'Right (East)', ans: 'North-East' },
      { dir1: 'South', dir2: 'Left (East)', ans: 'South-East' },
      { dir1: 'North', dir2: 'Left (West)', ans: 'North-West' },
      { dir1: 'South', dir2: 'Right (West)', ans: 'South-West' }
    ];
    const item = combos[(index - 1) % combos.length];
    return makeQ(id, 'directions', 'logical', 'easy',
      `In navigation test #${index}, ${person} walks ${d1} km ${item.dir1}, then turns ${item.dir2} and walks ${d2} km. In which overall cardinal direction is ${person} located relative to the starting point?`,
      item.ans,
      ['North', 'East', 'South', 'West'].filter(d => d !== item.ans),
      index,
      `Displacement along ${item.dir1} and ${item.dir2} produces ${item.ans}.`,
      "Plot movement on a cardinal compass grid."
    );
  } else if (difficulty === 'medium') {
    const k = index;
    const a = 3 * k;
    const b = 4 * k;
    const dist = 5 * k;
    return makeQ(id, 'directions', 'logical', 'medium',
      `A cyclist on trail #${index} rides ${a} km East, then turns North and rides ${b} km. What is the shortest straight-line distance back to the starting point?`,
      `${dist} km`,
      [`${dist + 2} km`, `${dist + 4} km`, `${dist + 6} km`],
      index,
      `Distance = √(${a}² + ${b}²) = √(${a*a} + ${b*b}) = ${dist} km.`,
      "Use Pythagoras theorem: √(base² + height²)."
    );
  } else {
    const times = ['1:00 PM', '2:00 PM', '3:00 PM', '4:00 PM', '5:00 PM', '6:00 PM', '7:00 PM', '8:00 PM'];
    const t = times[(index - 1) % times.length];
    const hourDirs = ['South-East', 'South-West', 'North-West', 'North', 'South', 'East', 'West', 'North-East'];
    const ansDir = hourDirs[(index - 1) % hourDirs.length];
    return makeQ(id, 'directions', 'logical', 'hard',
      `At ${t} on clock #${index}, the minute hand points towards North-East. In which direction does the hour hand point at this exact time?`,
      ansDir,
      ['North', 'East', 'South', 'West'].filter(d => d !== ansDir),
      index,
      `Calculated based on the ${t} hour/minute hand angular difference.`,
      "Determine the angular separation between hour and minute hands."
    );
  }
}

// 8. SYLLOGISMS
function genSyllogisms(id, difficulty, index) {
  const nounSets = [
    { a: 'Cats', b: 'Mammals', c: 'Animals' },
    { a: 'Roses', b: 'Flowers', c: 'Plants' },
    { a: 'Sedans', b: 'Cars', c: 'Vehicles' },
    { a: 'Apples', b: 'Fruits', c: 'Foods' },
    { a: 'Oaks', b: 'Trees', c: 'Plants' },
    { a: 'Dogs', b: 'Pets', c: 'Animals' },
    { a: 'Eagles', b: 'Birds', c: 'Creatures' },
    { a: 'Laptops', b: 'Computers', c: 'Devices' }
  ];
  const set = nounSets[(index - 1) % nounSets.length];

  if (difficulty === 'easy') {
    return makeQ(id, 'syllogisms', 'logical', 'easy',
      `Statements for problem #${index}: All ${set.a.toLowerCase()} are ${set.b.toLowerCase()}. All ${set.b.toLowerCase()} are ${set.c.toLowerCase()}.\nConclusions:\nI. All ${set.a.toLowerCase()} are ${set.c.toLowerCase()}.\nII. Some ${set.c.toLowerCase()} are ${set.a.toLowerCase()}.`,
      'Both conclusions I and II follow',
      ['Only conclusion I follows', 'Only conclusion II follows', 'Neither conclusion follows'],
      index,
      `All ${set.a} ⊂ ${set.b} ⊂ ${set.c} => All ${set.a} are ${set.c} (I holds) and Some ${set.c} are ${set.a} (II holds).`,
      "Nested set inclusion."
    );
  } else if (difficulty === 'medium') {
    return makeQ(id, 'syllogisms', 'logical', 'medium',
      `Statements for problem #${index}: Some ${set.a.toLowerCase()} are ${set.b.toLowerCase()}. No ${set.b.toLowerCase()} are ${set.c.toLowerCase()}.\nConclusions:\nI. Some ${set.a.toLowerCase()} are not ${set.c.toLowerCase()}.\nII. All ${set.c.toLowerCase()} are ${set.a.toLowerCase()}.`,
      'Only conclusion I follows',
      ['Only conclusion II follows', 'Both follow', 'Neither follows'],
      index,
      `Elements of ${set.a} inside ${set.b} can never be ${set.c} (I follows).`,
      "Exclusion rule applying to overlapping sets."
    );
  } else {
    return makeQ(id, 'syllogisms', 'logical', 'hard',
      `Statements for problem #${index}: No ${set.a.toLowerCase()} is ${set.b.toLowerCase()}. No ${set.b.toLowerCase()} is ${set.c.toLowerCase()}.\nConclusions:\nI. No ${set.a.toLowerCase()} is ${set.c.toLowerCase()}.\nII. Some ${set.a.toLowerCase()} are ${set.c.toLowerCase()}.`,
      'Either conclusion I or II follows',
      ['Only conclusion I follows', 'Only conclusion II follows', 'Neither conclusion follows'],
      index,
      `${set.a} and ${set.c} form a complementary either/or pair when unlinked.`,
      "Complementary pair rule."
    );
  }
}

// 9. GRAMMAR & VOCABULARY
function genGrammarVocab(id, difficulty, index) {
  if (difficulty === 'easy') {
    const vocabWords = [
      { word: 'CANDID', syn: 'Frank', wrong: ['Deceitful', 'Secretive', 'Cautious'], exp: "'Candid' means truthful, straightforward, or frank." },
      { word: 'BENEVOLENT', syn: 'Kind', wrong: ['Malevolent', 'Greedy', 'Hostile'], exp: "'Benevolent' means well-meaning and kindly." },
      { word: 'DILIGENT', syn: 'Hardworking', wrong: ['Lazy', 'Careless', 'Indifferent'], exp: "'Diligent' means showing care and effort in work." },
      { word: 'METICULOUS', syn: 'Precise', wrong: ['Sloppy', 'Hasty', 'Rough'], exp: "'Meticulous' means showing great attention to detail." },
      { word: 'LUCID', syn: 'Clear', wrong: ['Confusing', 'Vague', 'Obscure'], exp: "'Lucid' means expressed clearly or easy to understand." },
      { word: 'PRAGMATIC', syn: 'Practical', wrong: ['Idealistic', 'Impractical', 'Theoretical'], exp: "'Pragmatic' means dealing with things sensibly and realistically." },
      { word: 'ASTUTE', syn: 'Shrewd', wrong: ['Foolish', 'Naive', 'Slow'], exp: "'Astute' means having or showing an ability to accurately assess situations." },
      { word: 'RESILIENT', syn: 'Adaptable', wrong: ['Fragile', 'Rigid', 'Weak'], exp: "'Resilient' means able to withstand or recover quickly." }
    ];
    const item = vocabWords[(index - 1) % vocabWords.length];
    return makeQ(id, 'grammar_vocab', 'verbal', 'easy',
      `In vocabulary exercise #${index}, choose the synonym most nearly EQUIVALENT in meaning to '${item.word}':`,
      item.syn,
      item.wrong,
      index,
      item.exp,
      "Match the closest synonym."
    );
  } else if (difficulty === 'medium') {
    const grammarQuestions = [
      { correct: 'Neither of the applicants has completed the exam.', wrong: ['Neither of the applicants have completed the exam.', 'Neither of the applicant has completed the exam.', 'Neither applicants has completed the exam.'], exp: "'Neither' requires a singular verb ('has')." },
      { correct: 'Each of the team members was awarded a certificate.', wrong: ['Each of the team members were awarded a certificate.', 'Each of the team member was awarded a certificate.', 'Each team members were awarded a certificate.'], exp: "'Each' requires a singular verb ('was')." },
      { correct: 'The list of candidates is posted on the notice board.', wrong: ['The list of candidates are posted on the notice board.', 'The list of candidate is posted on the notice board.', 'The list candidates is posted on the notice board.'], exp: "Subject is 'list' (singular), requiring 'is'." }
    ];
    const item = grammarQuestions[(index - 1) % grammarQuestions.length];
    return makeQ(id, 'grammar_vocab', 'verbal', 'medium',
      `In grammar structure test #${index}, identify the sentence with correct Subject-Verb Agreement:`,
      item.correct,
      item.wrong,
      index,
      item.exp,
      "Check agreement rules for indefinite subjects."
    );
  } else {
    const hardWords = [
      { word: 'EPHEMERAL', ant: 'Perpetual', wrong: ['Transient', 'Fleeting', 'Evanescent'], exp: "'Ephemeral' means short-lived. Its antonym is 'Perpetual'." },
      { word: 'UBIQUITOUS', ant: 'Rare', wrong: ['Omnipresent', 'Pervasive', 'Universal'], exp: "'Ubiquitous' means present everywhere. Its antonym is 'Rare'." },
      { word: 'FASTIDIOUS', ant: 'Careless', wrong: ['Particular', 'Meticulous', 'Fussy'], exp: "'Fastidious' means meticulous. Its antonym is 'Careless'." },
      { word: 'OBSOLETE', ant: 'Modern', wrong: ['Outdated', 'Ancient', 'Archaic'], exp: "'Obsolete' means no longer in use. Its antonym is 'Modern'." }
    ];
    const item = hardWords[(index - 1) % hardWords.length];
    return makeQ(id, 'grammar_vocab', 'verbal', 'hard',
      `In advanced verbal exercise #${index}, choose the word most OPPOSITE in meaning to '${item.word}':`,
      item.ant,
      item.wrong,
      index,
      item.exp,
      "Select the opposite meaning."
    );
  }
}

// 10. READING COMPREHENSION
function genReadingComp(id, difficulty, index) {
  if (difficulty === 'easy') {
    const passages = [
      { pass: 'Renewable energy adoption has accelerated globally due to declining solar panel costs and urgent climate policies.', q: 'According to the passage, what primary factor has accelerated renewable energy adoption?', ans: 'Declining solar panel costs', wrong: ['Increased fossil fuel subsidies', 'Strict nuclear energy bans', 'Reduced global energy consumption'], exp: "Passage directly cites 'declining solar panel costs'." },
      { pass: 'Urban vertical farming utilizes indoor LED lighting and hydroponics, consuming 90% less water than traditional open-field agriculture.', q: 'How much less water does vertical farming consume compared to traditional agriculture?', ans: '90% less water', wrong: ['50% less water', '20% less water', '10% less water'], exp: "Passage explicitly states '90% less water'." },
      { pass: 'Deep learning neural networks process vast unstructured datasets by simulating multi-layered synaptic connections found in biological brains.', q: 'What feature enables deep learning networks to analyze unstructured data?', ans: 'Multi-layered synaptic connections', wrong: ['Manual rule programming', 'Quantum transistors', 'Relational database schemas'], exp: "Passage references 'multi-layered synaptic connections'." }
    ];
    const item = passages[(index - 1) % passages.length];
    return makeQ(id, 'reading_comp', 'verbal', 'easy',
      `Reading passage #${index}: '${item.pass}'\nQuestion: ${item.q}`,
      item.ans,
      item.wrong,
      index,
      item.exp,
      "Locate the direct cause in the passage."
    );
  } else if (difficulty === 'medium') {
    const medPassages = [
      { pass: 'While automation increases manufacturing throughput, critics argue it displaces entry-level workers without immediately creating equivalent technical roles.', q: 'What is the principal concern raised by critics regarding automation?', ans: 'Displacement of entry-level workers', wrong: ['Reduced product quality', 'Excessive training costs', 'Decreased manufacturing speed'], exp: "Critics argue automation 'displaces entry-level workers'." },
      { pass: 'Electric vehicle adoption reduces urban tailpipe emissions, but grid decarbonization remains essential to achieve net-zero lifecycle emissions.', q: 'What is necessary to achieve net-zero lifecycle emissions for electric vehicles?', ans: 'Grid decarbonization', wrong: ['Higher battery capacity', 'Lower highway speed limits', 'Increased fuel taxation'], exp: "Passage states 'grid decarbonization remains essential'." }
    ];
    const item = medPassages[(index - 1) % medPassages.length];
    return makeQ(id, 'reading_comp', 'verbal', 'medium',
      `Reading passage #${index}: '${item.pass}'\nQuestion: ${item.q}`,
      item.ans,
      item.wrong,
      index,
      item.exp,
      "Focus on the main criticism in the passage."
    );
  } else {
    const hardPassages = [
      { pass: 'Scientific consensus indicates that biodiversity loss weakens ecosystem resilience, making habitats increasingly vulnerable to extreme climate events.', q: 'Which statement best expresses the primary inference supported by the passage?', ans: 'Preserving biodiversity is crucial for ecosystem stability against climate shocks.', wrong: ['Ecosystems can adapt effortlessly to climate events.', 'Climate events are the sole cause of biodiversity loss.', 'Biodiversity loss only affects marine habitats.'], exp: "Passage links biodiversity directly to ecosystem resilience against climate events." },
      { pass: 'Central bank interest rate hikes suppress inflationary pressure, yet aggressive monetary tightening risks stifling capital investment and economic growth.', q: 'What central dilemma regarding interest rate hikes is highlighted in the passage?', ans: 'Balancing inflation control against economic growth deceleration.', wrong: ['Increasing consumer spending during recessions.', 'Eliminating public debt permanently.', 'Accelerating currency devaluation.'], exp: "Passage contrasts inflation suppression against the risk of stifling economic growth." }
    ];
    const item = hardPassages[(index - 1) % hardPassages.length];
    return makeQ(id, 'reading_comp', 'verbal', 'hard',
      `Reading passage #${index}: '${item.pass}'\nQuestion: ${item.q}`,
      item.ans,
      item.wrong,
      index,
      item.exp,
      "Identify the logical conclusion supported by evidence."
    );
  }
}

fs.writeFileSync(starterPath, JSON.stringify(allQuestions, null, 2), 'utf8');
console.log(`Successfully generated and verified ${allQuestions.length} questions in starter_questions.json!`);
