const fs = require('fs');
const path = require('path');

const starterPath = path.join(__dirname, '../app/src/main/assets/starter_questions.json');

const topics = [
  { id: 'percentages', cat: 'quant', name: 'Percentages' },
  { id: 'ratios', cat: 'quant', name: 'Ratios & Proportions' },
  { id: 'averages', cat: 'quant', name: 'Averages' },
  { id: 'profit_loss', cat: 'quant', name: 'Profit and Loss' },
  { id: 'time_work', cat: 'quant', name: 'Time and Work' },
  { id: 'number_series', cat: 'logical', name: 'Number Series' },
  { id: 'directions', cat: 'logical', name: 'Direction Sense' },
  { id: 'syllogisms', cat: 'logical', name: 'Syllogisms' },
  { id: 'grammar_vocab', cat: 'verbal', name: 'Grammar & Vocabulary' },
  { id: 'reading_comp', cat: 'verbal', name: 'Reading Comprehension' }
];

const allQuestions = [];

topics.forEach(top => {
  const topicId = top.id;
  const categoryId = top.cat;

  // Easy (01 - 80)
  for (let i = 1; i <= 80; i++) {
    const num = i < 10 ? `0${i}` : `${i}`;
    const qId = `q_${topicId}_${num}`;
    allQuestions.push(generateQuestion(qId, topicId, categoryId, 'easy', i));
  }

  // Medium (81 - 160)
  for (let i = 81; i <= 160; i++) {
    const num = i < 10 ? `0${i}` : `${i}`;
    const qId = `q_${topicId}_${num}`;
    allQuestions.push(generateQuestion(qId, topicId, categoryId, 'medium', i));
  }

  // Hard (161 - 240)
  for (let i = 161; i <= 240; i++) {
    const num = i < 10 ? `0${i}` : `${i}`;
    const qId = `q_${topicId}_${num}`;
    allQuestions.push(generateQuestion(qId, topicId, categoryId, 'hard', i));
  }
});

function generateQuestion(id, topicId, categoryId, difficulty, index) {
  switch (topicId) {
    case 'percentages':
      return genPercentages(id, difficulty, index);
    case 'ratios':
      return genRatios(id, difficulty, index);
    case 'averages':
      return genAverages(id, difficulty, index);
    case 'profit_loss':
      return genProfitLoss(id, difficulty, index);
    case 'time_work':
      return genTimeWork(id, difficulty, index);
    case 'number_series':
      return genNumberSeries(id, difficulty, index);
    case 'directions':
      return genDirections(id, difficulty, index);
    case 'syllogisms':
      return genSyllogisms(id, difficulty, index);
    case 'grammar_vocab':
      return genGrammarVocab(id, difficulty, index);
    case 'reading_comp':
      return genReadingComp(id, difficulty, index);
    default:
      throw new Error(`Unknown topic: ${topicId}`);
  }
}

// 1. PERCENTAGES
function genPercentages(id, difficulty, index) {
  if (difficulty === 'easy') {
    const base = index * 15 + 50;
    const pct = ((index % 8) + 1) * 5; // 5%, 10%, 15% ... 40%
    const ans = (base * pct) / 100;
    return makeQ(id, 'percentages', 'quant', 'easy',
      `What is ${pct}% of ${base}?`,
      [`${ans - 5}`, `${ans}`, `${ans + 5}`, `${ans + 10}`],
      'b',
      `${pct}% of ${base} = (${pct} / 100) × ${base} = ${ans}.`,
      `Multiply ${base} by ${pct / 100}.`
    );
  } else if (difficulty === 'medium') {
    const val = index * 10 + 100;
    const inc = ((index % 5) + 1) * 5;
    const finalVal = val * (1 + inc / 100);
    return makeQ(id, 'percentages', 'quant', 'medium',
      `An item originally costing $${val} is increased in price by ${inc}%. What is the new selling price?`,
      [`$${finalVal - 10}`, `$${finalVal}`, `$${finalVal + 10}`, `$${finalVal + 15}`],
      'b',
      `Increase = ${inc}% of $${val} = $${(val * inc) / 100}.\nNew Price = $${val} + $${(val * inc) / 100} = $${finalVal}.`,
      `New Price = Original × (1 + ${inc}/100).`
    );
  } else {
    const orig = index * 20 + 200;
    const p1 = 10 + (index % 4) * 5;
    const p2 = 5 + (index % 3) * 5;
    const afterInc = orig * (1 + p1 / 100);
    const netVal = afterInc * (1 - p2 / 100);
    const diffPct = (((netVal - orig) / orig) * 100).toFixed(1);
    const isGain = netVal >= orig;
    return makeQ(id, 'percentages', 'quant', 'hard',
      `A salary of $${orig} is increased by ${p1}% and then decreased by ${p2}%. What is the net percentage change in salary?`,
      [`${Math.abs(diffPct)}% ${isGain ? 'increase' : 'decrease'}`, `${Math.abs(diffPct) + 2}% increase`, `${Math.abs(diffPct) + 3}% decrease`, `${Math.abs(diffPct) + 5}% increase`],
      'a',
      `1. After ${p1}% increase: $${orig} × ${(1 + p1 / 100).toFixed(2)} = $${afterInc.toFixed(2)}.\n2. After ${p2}% decrease: $${afterInc.toFixed(2)} × ${(1 - p2 / 100).toFixed(2)} = $${netVal.toFixed(2)}.\n3. Net change = ${diffPct}%.`,
      `Multiply successive factors: ${(1 + p1 / 100).toFixed(2)} × ${(1 - p2 / 100).toFixed(2)}.`
    );
  }
}

// 2. RATIOS
function genRatios(id, difficulty, index) {
  if (difficulty === 'easy') {
    const a = (index % 5) + 1;
    const b = a + 1;
    const total = (a + b) * (index * 2 + 10);
    const shareA = (a / (a + b)) * total;
    const shareB = (b / (a + b)) * total;
    return makeQ(id, 'ratios', 'quant', 'easy',
      `A total of $${total} is divided between Person A and Person B in the ratio ${a}:${b}. How much does Person A receive?`,
      [`$${shareA - 5}`, `$${shareA}`, `$${shareB}`, `$${total - 10}`],
      'b',
      `Total parts = ${a} + ${b} = ${a + b}.\nValue per part = $${total} / ${a + b} = $${total / (a + b)}.\nPerson A's share = ${a} × $${total / (a + b)} = $${shareA}.`,
      `Person A's share = (${a} / ${a + b}) × $${total}.`
    );
  } else if (difficulty === 'medium') {
    const total = index * 40 + 200;
    const shareC = (5 / 10) * total;
    return makeQ(id, 'ratios', 'quant', 'medium',
      `An amount of $${total} is distributed among A, B, and C in the ratio 2:3:5. What is C's share?`,
      [`$${shareC - 20}`, `$${shareC}`, `$${shareC + 20}`, `$${shareC + 50}`],
      'b',
      `Total ratio parts = 2 + 3 + 5 = 10.\nC's fraction = 5 / 10 = 1/2.\nC's share = $${total} / 2 = $${shareC}.`,
      "C gets 5 out of 10 equal parts of the total sum."
    );
  } else {
    const m1 = index * 3 + 15;
    const m2 = index * 2 + 10;
    const diff = m1 - m2;
    return makeQ(id, 'ratios', 'quant', 'hard',
      `In a mixture of ${m1 + m2} liters, the ratio of milk to water is 3:2. How many liters of water must be added to make the ratio of milk to water 1:1?`,
      [`${diff} liters`, `${diff + 3} liters`, `${m1} liters`, `${m2} liters`],
      'a',
      `Milk = 3/5 × ${m1 + m2} = ${m1} liters.\nWater = 2/5 × ${m1 + m2} = ${m2} liters.\nTo make ratio 1:1, Water must equal Milk (${m1} liters).\nWater to add = ${m1} - ${m2} = ${diff} liters.`,
      "Calculate initial quantity of milk and water, then find additional water needed."
    );
  }
}

// 3. AVERAGES
function genAverages(id, difficulty, index) {
  if (difficulty === 'easy') {
    const num1 = index * 4 + 10;
    const num2 = index * 4 + 20;
    const num3 = index * 4 + 30;
    const avg = (num1 + num2 + num3) / 3;
    return makeQ(id, 'averages', 'quant', 'easy',
      `What is the average of ${num1}, ${num2}, and ${num3}?`,
      [`${avg - 4}`, `${avg}`, `${avg + 4}`, `${avg + 8}`],
      'b',
      `Average = (${num1} + ${num2} + ${num3}) / 3 = ${num1 + num2 + num3} / 3 = ${avg}.`,
      "Add all numbers together and divide by 3."
    );
  } else if (difficulty === 'medium') {
    const count = 5 + (index % 3);
    const initialAvg = index * 2 + 40;
    const newNum = initialAvg + (index % 5 + 1) * (count + 1);
    const newAvg = (initialAvg * count + newNum) / (count + 1);
    return makeQ(id, 'averages', 'quant', 'medium',
      `The average score of ${count} students in a test is ${initialAvg}. If a new student scores ${newNum}, what is the new average score of all ${count + 1} students?`,
      [`${newAvg - 2}`, `${newAvg}`, `${newAvg + 2}`, `${newAvg + 4}`],
      'b',
      `Initial total score = ${count} × ${initialAvg} = ${count * initialAvg}.\nNew total score = ${count * initialAvg} + ${newNum} = ${count * initialAvg + newNum}.\nNew average = ${count * initialAvg + newNum} / ${count + 1} = ${newAvg}.`,
      `Find total score of first ${count} students, add new score, then divide by ${count + 1}.`
    );
  } else {
    const s1 = 30 + (index % 10) * 5;
    const s2 = s1 + 20;
    const avgSpeed = ((2 * s1 * s2) / (s1 + s2)).toFixed(1);
    return makeQ(id, 'averages', 'quant', 'hard',
      `A motorist travels from Town A to Town B at a speed of ${s1} km/h and returns along the same route at ${s2} km/h. What is the average speed for the entire round trip?`,
      [`${avgSpeed} km/h`, `${(s1 + s2) / 2} km/h`, `${(s1 + 5)} km/h`, `${(s2 - 5)} km/h`],
      'a',
      `Formula: Average Speed = (2 × s1 × s2) / (s1 + s2) = (2 × ${s1} × ${s2}) / (${s1} + ${s2}) = ${avgSpeed} km/h.`,
      "Use harmonic mean formula 2s1s2 / (s1 + s2) when equal distances are covered."
    );
  }
}

// 4. PROFIT & LOSS
function genProfitLoss(id, difficulty, index) {
  if (difficulty === 'easy') {
    const cp = index * 25 + 100;
    const sp = cp + (index % 5 + 1) * 20;
    const profitPct = (((sp - cp) / cp) * 100).toFixed(1);
    return makeQ(id, 'profit_loss', 'quant', 'easy',
      `A merchant buys an item for $${cp} and sells it for $${sp}. What is the profit percentage?`,
      [`${profitPct}%`, `${(parseFloat(profitPct) + 5).toFixed(1)}%`, `${(parseFloat(profitPct) - 2).toFixed(1)}%`, `${(parseFloat(profitPct) + 8).toFixed(1)}%`],
      'a',
      `Profit = $${sp} - $${cp} = $${sp - cp}.\nProfit % = (Profit / Cost Price) × 100 = (${sp - cp} / ${cp}) × 100 = ${profitPct}%.`,
      "Profit % = [(SP - CP) / CP] × 100."
    );
  } else if (difficulty === 'medium') {
    const cp = index * 30 + 150;
    const pPct = 5 + (index % 4) * 5;
    const sp = cp * (1 + pPct / 100);
    return makeQ(id, 'profit_loss', 'quant', 'medium',
      `An article bought for $${cp} is sold at a profit of ${pPct}%. What is the selling price?`,
      [`$${sp - 10}`, `$${sp}`, `$${sp + 10}`, `$${sp + 15}`],
      'b',
      `Profit = ${pPct}% of $${cp} = $${(cp * pPct) / 100}.\nSelling Price = $${cp} + $${(cp * pPct) / 100} = $${sp}.`,
      "Selling Price = CP × (1 + Profit%/100)."
    );
  } else {
    const mp = index * 50 + 400;
    const dPct = 10 + (index % 3) * 5; // 10%, 15%, 20%
    const pPct = 15 + (index % 4) * 5; // 15%, 20%, 25%, 30%
    const sp = mp * (1 - dPct / 100);
    const cp = (sp / (1 + pPct / 100)).toFixed(0);
    return makeQ(id, 'profit_loss', 'quant', 'hard',
      `A shopkeeper marks an item at $${mp}. After offering a ${dPct}% discount on the marked price, he still makes a ${pPct}% profit. What was the cost price of the item?`,
      [`$${cp}`, `$${parseInt(cp) + 20}`, `$${parseInt(cp) - 15}`, `$${parseInt(cp) + 35}`],
      'a',
      `1. Selling Price = Marked Price × ${(1 - dPct / 100).toFixed(2)} = $${mp} × ${(1 - dPct / 100).toFixed(2)} = $${sp}.\n2. Cost Price = Selling Price / ${(1 + pPct / 100).toFixed(2)} = $${sp} / ${(1 + pPct / 100).toFixed(2)} ≈ $${cp}.`,
      "First find SP using discount on Marked Price, then calculate CP from profit percentage."
    );
  }
}

// 5. TIME & WORK
function genTimeWork(id, difficulty, index) {
  if (difficulty === 'easy') {
    const da = 10 + (index % 10) * 2;
    const db = da * 2;
    const comb = ((da * db) / (da + db)).toFixed(1);
    return makeQ(id, 'time_work', 'quant', 'easy',
      `Worker A can complete a task in ${da} days and Worker B can complete the same task in ${db} days. How many days will they take working together?`,
      [`${comb} days`, `${(parseFloat(comb) + 2).toFixed(1)} days`, `${(da / 2).toFixed(1)} days`, `${(db / 2).toFixed(1)} days`],
      'a',
      `Worker A's 1-day rate = 1/${da}.\nWorker B's 1-day rate = 1/${db}.\nCombined rate = 1/${da} + 1/${db} = (${db} + ${da})/(${da * db}).\nDays required = (${da} × ${db}) / (${da} + ${db}) = ${comb} days.`,
      "Combined time = (A × B) / (A + B)."
    );
  } else if (difficulty === 'medium') {
    const da = 12 + (index % 6) * 2;
    const db = 24 + (index % 6) * 2;
    const k = 2 + (index % 3);
    const rateA = 1 / da;
    const rateB = 1 / db;
    const frac = ((rateA + rateB) * k).toFixed(2);
    return makeQ(id, 'time_work', 'quant', 'medium',
      `Worker A finishes a project in ${da} days. Worker B finishes it in ${db} days. Working together, what decimal fraction of the project do they finish in ${k} days?`,
      [`${frac}`, `${(parseFloat(frac) + 0.10).toFixed(2)}`, `${(parseFloat(frac) - 0.05).toFixed(2)}`, `${(parseFloat(frac) + 0.15).toFixed(2)}`],
      'a',
      `Rate of A = 1/${da}, Rate of B = 1/${db}.\nCombined 1-day rate = 1/${da} + 1/${db}.\nIn ${k} days = ${k} × (1/${da} + 1/${db}) = ${frac}.`,
      `Multiply combined daily rate by ${k} days.`
    );
  } else {
    const h1 = 6 + (index % 5) * 2;
    const h2 = h1 + 4;
    const h3 = h1 + h2;
    const netRate = 1 / h1 + 1 / h2 - 1 / h3;
    const totalHours = (1 / netRate).toFixed(1);
    return makeQ(id, 'time_work', 'quant', 'hard',
      `Pipe A fills a tank in ${h1} hours and Pipe B fills it in ${h2} hours. Pipe C empties the full tank in ${h3} hours. If all three pipes are opened simultaneously, how long will it take to fill the tank?`,
      [`${totalHours} hours`, `${(parseFloat(totalHours) + 2).toFixed(1)} hours`, `${(parseFloat(totalHours) - 1.5).toFixed(1)} hours`, `${(parseFloat(totalHours) + 4).toFixed(1)} hours`],
      'a',
      `Rate of A = +1/${h1}, Rate of B = +1/${h2}, Rate of C = -1/${h3}.\nNet rate = 1/${h1} + 1/${h2} - 1/${h3} = ${netRate.toFixed(3)} per hour.\nTotal hours = 1 / ${netRate.toFixed(3)} = ${totalHours} hours.`,
      "Add filling rates and subtract emptying rate to get net hourly filling rate."
    );
  }
}

// 6. NUMBER SERIES
function genNumberSeries(id, difficulty, index) {
  if (difficulty === 'easy') {
    const start = index * 3 + 5;
    const step = 3 + (index % 7);
    const s1 = start;
    const s2 = start + step;
    const s3 = start + step * 2;
    const s4 = start + step * 3;
    const ans = start + step * 4;
    return makeQ(id, 'number_series', 'logical', 'easy',
      `Find the next number in the series: ${s1}, ${s2}, ${s3}, ${s4}, ?`,
      [`${ans - 2}`, `${ans}`, `${ans + 2}`, `${ans + 4}`],
      'b',
      `This is an arithmetic progression with a constant common difference of +${step}.\n${s4} + ${step} = ${ans}.`,
      `Identify the constant difference between consecutive terms (+${step}).`
    );
  } else if (difficulty === 'medium') {
    const start = index * 2 + 1;
    const s1 = start + 1 * 2;
    const s2 = start + 2 * 3;
    const s3 = start + 3 * 4;
    const s4 = start + 4 * 5;
    const ans = start + 5 * 6;
    return makeQ(id, 'number_series', 'logical', 'medium',
      `Identify the missing term in the sequence: ${s1}, ${s2}, ${s3}, ${s4}, ?`,
      [`${ans - 2}`, `${ans}`, `${ans + 2}`, `${ans + 6}`],
      'b',
      `Pattern: Terms increase by 1×2, 2×3, 3×4, 4×5, 5×6.\nNext term = ${s4} + 10 = ${ans}.`,
      "Examine the consecutive differences: +4, +6, +8... next is +10."
    );
  } else {
    const start = index + 2;
    const k = (index % 4) + 2;
    const s1 = start;
    const s2 = start + k;
    const s3 = start + k + 2 * k;
    const s4 = start + k + 2 * k + 4 * k;
    const ans = start + k + 2 * k + 4 * k + 8 * k;
    return makeQ(id, 'number_series', 'logical', 'hard',
      `Find the next number in the series: ${s1}, ${s2}, ${s3}, ${s4}, ?`,
      [`${ans}`, `${ans - 2}`, `${ans + 4}`, `${ans + 6}`],
      'a',
      `Pattern: Differences double each step (+${k}, +${2 * k}, +${4 * k}, +${8 * k}).\n${s4} + ${8 * k} = ${ans}.`,
      "Analyze the differences between terms: each difference doubles."
    );
  }
}

// 7. DIRECTIONS
function genDirections(id, difficulty, index) {
  if (difficulty === 'easy') {
    const names = ['Alex', 'Brian', 'Clara', 'David', 'Elena', 'Frank', 'Grace', 'Henry'];
    const person = names[(index - 1) % names.length];
    const d1 = 3 + (index % 10);
    const d2 = 3 + (index % 10);
    const directions = [
      { d1: 'North', d2: 'Right (East)', ans: 'North-East', exp: 'North (+Y) and East (+X) displacement produces North-East.' },
      { d1: 'South', d2: 'Left (East)', ans: 'South-East', exp: 'South (-Y) and East (+X) displacement produces South-East.' },
      { d1: 'North', d2: 'Left (West)', ans: 'North-West', exp: 'North (+Y) and West (-X) displacement produces North-West.' },
      { d1: 'South', d2: 'Right (West)', ans: 'South-West', exp: 'South (-Y) and West (-X) displacement produces South-West.' }
    ];
    const item = directions[(index - 1) % directions.length];
    return makeQ(id, 'directions', 'logical', 'easy',
      `${person} walks ${d1} km ${item.d1}, then turns ${item.d2} and walks ${d2} km. In which direction is ${person} relative to the starting point?`,
      [item.ans, 'North', 'East', 'South'],
      'a',
      `1. Walking ${d1} km ${item.d1}.\n2. Turning ${item.d2} for ${d2} km.\n3. Resultant direction is ${item.ans}.`,
      `Draw the movement on a compass grid.`
    );
  } else if (difficulty === 'medium') {
    const k = (index % 10) + 1;
    const a = 3 * k;
    const b = 4 * k;
    const dist = 5 * k;
    return makeQ(id, 'directions', 'logical', 'medium',
      `A cyclist travels ${a} km East, then turns North and travels ${b} km. What is the shortest straight-line distance from the starting point?`,
      [`${dist} km`, `${dist + 2} km`, `${dist + 4} km`, `${a + b} km`],
      'a',
      `Path forms a right-angled triangle with leg lengths ${a} km and ${b} km.\nPythagoras Theorem: Distance = √(${a}² + ${b}²) = √(${a * a} + ${b * b}) = √${dist * dist} = ${dist} km.`,
      "Use Pythagoras Theorem: hypotenuse = √(base² + height²)."
    );
  } else {
    const hour = (index % 12) + 1;
    const rotatedDirs = [
      { start: '12:00 PM', handDir: 'North-East', hourDir: 'North-East', exp: 'At 12:00 PM both hands point together towards North-East.' },
      { start: '3:00 PM', handDir: 'North-East', hourDir: 'South-East', exp: 'At 3:00 PM, hands are 90° apart clockwise. 90° clockwise from North-East is South-East.' },
      { start: '6:00 PM', handDir: 'North-East', hourDir: 'South-West', exp: 'At 6:00 PM, hands are 180° opposite. 180° from North-East is South-West.' },
      { start: '9:00 PM', handDir: 'North-East', hourDir: 'North-West', exp: 'At 9:00 PM, hands are 270° clockwise (90° counter-clockwise). 90° counter-clockwise from North-East is North-West.' }
    ];
    const item = rotatedDirs[(index - 1) % rotatedDirs.length];
    return makeQ(id, 'directions', 'logical', 'hard',
      `At ${item.start}, the minute hand of a clock points towards ${item.handDir}. In which direction does the hour hand point?`,
      [item.hourDir, 'North', 'South-East', 'West'],
      'a',
      item.exp,
      "Observe the angle between hour and minute hands at the given clock time."
    );
  }
}

// 8. SYLLOGISMS
function genSyllogisms(id, difficulty, index) {
  const sets = [
    { a: 'Cats', b: 'Mammals', c: 'Animals' },
    { a: 'Roses', b: 'Flowers', c: 'Plants' },
    { a: 'Cars', b: 'Vehicles', c: 'Machines' },
    { a: 'Apples', b: 'Fruits', c: 'Foods' },
    { a: 'Oaks', b: 'Trees', c: 'Plants' },
    { a: 'Dogs', b: 'Pets', c: 'Animals' },
    { a: 'Eagles', b: 'Birds', c: 'Creatures' },
    { a: 'Laptops', b: 'Computers', c: 'Devices' }
  ];
  const subj = sets[(index - 1) % sets.length];

  if (difficulty === 'easy') {
    return makeQ(id, 'syllogisms', 'logical', 'easy',
      `Statements: All ${subj.a.toLowerCase()} are ${subj.b.toLowerCase()}. All ${subj.b.toLowerCase()} are ${subj.c.toLowerCase()}.\nConclusions:\nI. All ${subj.a.toLowerCase()} are ${subj.c.toLowerCase()}.\nII. Some ${subj.c.toLowerCase()} are ${subj.a.toLowerCase()}.`,
      ['Both conclusions I and II follow', 'Only conclusion I follows', 'Only conclusion II follows', 'Neither conclusion follows'],
      'a',
      `1. All ${subj.a} ⊂ ${subj.b} ⊂ ${subj.c} => All ${subj.a} are ${subj.c} (I is valid).\n2. Since ${subj.a} exist inside ${subj.c}, Some ${subj.c} are ${subj.a} (II is valid).`,
      `Use nested sets (${subj.a} inside ${subj.b} inside ${subj.c}).`
    );
  } else if (difficulty === 'medium') {
    return makeQ(id, 'syllogisms', 'logical', 'medium',
      `Statements: Some ${subj.a.toLowerCase()} are ${subj.b.toLowerCase()}. No ${subj.b.toLowerCase()} are ${subj.c.toLowerCase()}.\nConclusions:\nI. Some ${subj.a.toLowerCase()} are not ${subj.c.toLowerCase()}.\nII. All ${subj.c.toLowerCase()} are ${subj.a.toLowerCase()}.`,
      ['Only conclusion I follows', 'Only conclusion II follows', 'Both follow', 'Neither follows'],
      'a',
      `1. The ${subj.a.toLowerCase()} that overlap with ${subj.b.toLowerCase()} CANNOT be ${subj.c.toLowerCase()} (I follows).\n2. No universal inclusion for ${subj.c.toLowerCase()} into ${subj.a.toLowerCase()} (II does not follow).`,
      `Elements of ${subj.a} inside ${subj.b} can never overlap with ${subj.c}.`
    );
  } else {
    return makeQ(id, 'syllogisms', 'logical', 'hard',
      `Statements: No ${subj.a.toLowerCase()} is ${subj.b.toLowerCase()}. No ${subj.b.toLowerCase()} is ${subj.c.toLowerCase()}.\nConclusions:\nI. No ${subj.a.toLowerCase()} is ${subj.c.toLowerCase()}.\nII. Some ${subj.a.toLowerCase()} are ${subj.c.toLowerCase()}.`,
      ['Either conclusion I or II follows', 'Only conclusion I follows', 'Only conclusion II follows', 'Neither conclusion follows'],
      'a',
      `No direct relationship is established between ${subj.a} and ${subj.c}.\nHowever, they form a complementary pair (either they share no elements or at least one element).\nTherefore, Either I or II must follow.`,
      "Two unlinked sets form an 'Either/Or' complementary pair."
    );
  }
}

// 9. GRAMMAR & VOCABULARY
function genGrammarVocab(id, difficulty, index) {
  if (difficulty === 'easy') {
    const vocabList = [
      { word: 'CANDID', ans: 'Frank', options: ['Frank', 'Deceitful', 'Secretive', 'Cautious'], exp: "'Candid' means truthful, straightforward, or frank in expression." },
      { word: 'BENEVOLENT', ans: 'Kind', options: ['Kind', 'Malevolent', 'Greedy', 'Hostile'], exp: "'Benevolent' means well-meaning and kindly." },
      { word: 'DILIGENT', ans: 'Hardworking', options: ['Hardworking', 'Lazy', 'Careless', 'Indifferent'], exp: "'Diligent' means showing care and effort in work." },
      { word: 'METICULOUS', ans: 'Precise', options: ['Precise', 'Sloppy', 'Hasty', 'Rough'], exp: "'Meticulous' means showing great attention to detail." },
      { word: 'LUCID', ans: 'Clear', options: ['Clear', 'Confusing', 'Vague', 'Obscure'], exp: "'Lucid' means expressed clearly or easy to understand." },
      { word: 'PRAGMATIC', ans: 'Practical', options: ['Practical', 'Idealistic', 'Impractical', 'Theoretical'], exp: "'Pragmatic' means dealing with things sensibly and realistically." },
      { word: 'ASTUTE', ans: 'Shrewd', options: ['Shrewd', 'Foolish', 'Naive', 'Slow'], exp: "'Astute' means having or showing an ability to accurately assess situations." },
      { word: 'RESILIENT', ans: 'Adaptable', options: ['Adaptable', 'Fragile', 'Rigid', 'Weak'], exp: "'Resilient' means able to withstand or recover quickly from difficult conditions." }
    ];
    const item = vocabList[(index - 1) % vocabList.length];
    return makeQ(id, 'grammar_vocab', 'verbal', 'easy',
      `Choose the synonym of '${item.word}':`,
      item.options,
      'a',
      item.exp,
      "Select the option closest in meaning."
    );
  } else if (difficulty === 'medium') {
    const sentenceList = [
      { correct: 'Neither of the candidates has answered.', wrong1: 'The list of items are on the table.', wrong2: 'The committee have decided its final verdict.', wrong3: 'Every student and teacher were present.', exp: "'Neither' takes a singular verb ('has')." },
      { correct: 'Each of the team members was awarded a certificate.', wrong1: 'Each of the team members were awarded a certificate.', wrong2: 'Each of the team member was awarded a certificate.', wrong3: 'Each team members were awarded a certificate.', exp: "'Each' requires a singular verb ('was')." },
      { correct: 'Neither John nor his brothers were present.', wrong1: 'Neither John nor his brothers was present.', wrong2: 'Neither John nor his brothers is present.', wrong3: 'Neither John nor his brother were present.', exp: "Verb agrees with the closer subject ('brothers' -> 'were')." }
    ];
    const item = sentenceList[(index - 1) % sentenceList.length];
    return makeQ(id, 'grammar_vocab', 'verbal', 'medium',
      `Identify the correct sentence with proper Subject-Verb Agreement:`,
      [item.correct, item.wrong1, item.wrong2, item.wrong3],
      'a',
      item.exp,
      "Indefinite pronouns like 'neither' and 'each' require singular verbs."
    );
  } else {
    const hardVocab = [
      { word: 'EPHEMERAL', ans: 'Perpetual', options: ['Perpetual', 'Transient', 'Fleeting', 'Evanescent'], exp: "'Ephemeral' means short-lived. Its opposite is 'Perpetual'." },
      { word: 'UBIQUITOUS', ans: 'Rare', options: ['Rare', 'Omnipresent', 'Pervasive', 'Universal'], exp: "'Ubiquitous' means present everywhere. Its opposite is 'Rare'." },
      { word: 'FASTIDIOUS', ans: 'Careless', options: ['Careless', 'Particular', 'Meticulous', 'Fussy'], exp: "'Fastidious' means very attentive to detail. Its opposite is 'Careless'." },
      { word: 'OBSOLETE', ans: 'Modern', options: ['Modern', 'Outdated', 'Ancient', 'Archal'], exp: "'Obsolete' means no longer produced or used. Its opposite is 'Modern'." }
    ];
    const item = hardVocab[(index - 1) % hardVocab.length];
    return makeQ(id, 'grammar_vocab', 'verbal', 'hard',
      `Choose the word most OPPOSITE in meaning to '${item.word}':`,
      item.options,
      'a',
      item.exp,
      "Look for a word that means the exact opposite."
    );
  }
}

// 10. READING COMPREHENSION
function genReadingComp(id, difficulty, index) {
  if (difficulty === 'easy') {
    const passages = [
      { pass: 'Renewable energy adoption has accelerated globally due to declining solar panel costs and urgent climate policies.', q: 'According to the passage, what is a primary driver of renewable energy adoption?', ans: 'Declining solar panel costs', options: ['Declining solar panel costs', 'Increased fossil fuel subsidies', 'Strict nuclear energy bans', 'Reduced energy consumption'], exp: "Passage directly cites 'declining solar panel costs'." },
      { pass: 'Urban vertical farming utilizes LED lighting and hydroponics to cultivate crops indoors, consuming 90% less water than traditional agriculture.', q: 'How much less water does vertical farming consume compared to traditional farming?', ans: '90% less water', options: ['90% less water', '50% less water', '20% less water', '10% less water'], exp: "Passage directly states 'consuming 90% less water'." },
      { pass: 'Deep learning neural networks process vast unstructured datasets by simulating multi-layered synaptic connections found in biological brains.', q: 'What enables deep learning networks to process unstructured data?', ans: 'Multi-layered synaptic connections', options: ['Multi-layered synaptic connections', 'Manual rule programming', 'Quantum transistors', 'Relational database schemas'], exp: "Passage directly references 'multi-layered synaptic connections'." }
    ];
    const item = passages[(index - 1) % passages.length];
    return makeQ(id, 'reading_comp', 'verbal', 'easy',
      `Passage: '${item.pass}'\nQuestion: ${item.q}`,
      item.options,
      'a',
      item.exp,
      "Locate the direct cause mentioned in the sentence."
    );
  } else if (difficulty === 'medium') {
    const medPassages = [
      { pass: 'While automation increases manufacturing throughput, critics argue it displaces entry-level workers without immediately creating equivalent technical roles.', q: 'What is the main concern raised by critics regarding automation?', ans: 'Displacement of entry-level workers', options: ['Displacement of entry-level workers', 'Reduced product quality', 'Excessive training costs', 'Decreased manufacturing speed'], exp: "Critics argue automation 'displaces entry-level workers'." },
      { pass: 'Electric vehicle adoption reduces urban tailpipe emissions, but grid decarbonization remains essential to achieve net-zero lifecycle emissions.', q: 'What is necessary to achieve net-zero lifecycle emissions for electric vehicles?', ans: 'Grid decarbonization', options: ['Grid decarbonization', 'Higher battery capacity', 'Lower highway speed limits', 'Increased fuel taxation'], exp: "Passage states 'grid decarbonization remains essential'." }
    ];
    const item = medPassages[(index - 1) % medPassages.length];
    return makeQ(id, 'reading_comp', 'verbal', 'medium',
      `Passage: '${item.pass}'\nQuestion: ${item.q}`,
      item.options,
      'a',
      item.exp,
      "Focus on the main argument or condition stated in the passage."
    );
  } else {
    const hardPassages = [
      { pass: 'Scientific consensus indicates that biodiversity loss weakens ecosystem resilience, making habitats increasingly vulnerable to extreme climate events.', q: 'Which statement best expresses the primary inference of the passage?', ans: 'Preserving biodiversity is crucial for ecosystem stability against climate shocks.', options: ['Preserving biodiversity is crucial for ecosystem stability against climate shocks.', 'Ecosystems can adapt effortlessly to climate events.', 'Climate events are the sole cause of biodiversity loss.', 'Biodiversity loss only affects marine habitats.'], exp: "The passage links biodiversity to ecosystem resilience against climate events." },
      { pass: 'Central bank interest rate hikes suppress inflationary pressure, yet aggressive monetary tightening risks stifling capital investment and economic growth.', q: 'What dilemma is highlighted regarding central bank interest rate hikes?', ans: 'Balancing inflation control against economic growth deceleration.', options: ['Balancing inflation control against economic growth deceleration.', 'Increasing consumer spending during recessions.', 'Eliminating public debt permanently.', 'Accelerating currency devaluation.'], exp: "Passage contrasts inflation suppression against the risk of stifling economic growth." }
    ];
    const item = hardPassages[(index - 1) % hardPassages.length];
    return makeQ(id, 'reading_comp', 'verbal', 'hard',
      `Passage: '${item.pass}'\nQuestion: ${item.q}`,
      item.options,
      'a',
      item.exp,
      "Derive the primary implication supported by the passage evidence."
    );
  }
}

function makeQ(id, topicId, categoryId, difficulty, questionText, optionTexts, correctId, explanation, hint) {
  return {
    id: id,
    topicId: topicId,
    categoryId: categoryId,
    difficulty: difficulty,
    languageCode: 'en',
    questionText: questionText,
    options: [
      { id: 'a', text: optionTexts[0] },
      { id: 'b', text: optionTexts[1] },
      { id: 'c', text: optionTexts[2] },
      { id: 'd', text: optionTexts[3] }
    ],
    correctOptionId: correctId,
    explanation: explanation,
    hint: hint,
    published: true,
    version: 1,
    updatedAt: "2026-09-19T12:00:00Z"
  };
}

fs.writeFileSync(starterPath, JSON.stringify(allQuestions, null, 2), 'utf8');
console.log(`Successfully generated and saved ${allQuestions.length} questions to starter_questions.json!`);
