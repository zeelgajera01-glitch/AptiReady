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

  // Easy (01 - 40)
  for (let i = 1; i <= 40; i++) {
    const num = i < 10 ? `0${i}` : `${i}`;
    const qId = `q_${topicId}_${num}`;
    allQuestions.push(generateQuestion(qId, topicId, categoryId, 'easy', i));
  }

  // Medium (41 - 80)
  for (let i = 41; i <= 80; i++) {
    const num = i < 10 ? `0${i}` : `${i}`;
    const qId = `q_${topicId}_${num}`;
    allQuestions.push(generateQuestion(qId, topicId, categoryId, 'medium', i));
  }

  // Hard (81 - 120)
  for (let i = 81; i <= 120; i++) {
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
    const base = index * 20 + 50;
    const pct = ((index % 5) + 1) * 10; // 10%, 20%, 30%, 40%, 50%
    const ans = (base * pct) / 100;
    return makeQ(id, 'percentages', 'quant', 'easy',
      `What is ${pct}% of ${base}?`,
      [`${ans - 5}`, `${ans}`, `${ans + 5}`, `${ans + 10}`],
      'b',
      `${pct}% of ${base} = (${pct} / 100) × ${base} = ${ans}.`,
      `Multiply ${base} by ${pct / 100}.`
    );
  } else if (difficulty === 'medium') {
    const val = index * 15 + 100;
    const inc = ((index % 4) + 1) * 10;
    const finalVal = val * (1 + inc / 100);
    return makeQ(id, 'percentages', 'quant', 'medium',
      `An item originally costing $${val} is increased in price by ${inc}%. What is the new selling price?`,
      [`$${finalVal - 10}`, `$${finalVal}`, `$${finalVal + 10}`, `$${finalVal + 15}`],
      'b',
      `Increase = ${inc}% of $${val} = $${(val * inc) / 100}.\nNew Price = $${val} + $${(val * inc) / 100} = $${finalVal}.`,
      `New Price = Original × (1 + ${inc}/100).`
    );
  } else {
    const orig = index * 25 + 200;
    const afterInc = orig * 1.20;
    const netVal = afterInc * 0.85;
    return makeQ(id, 'percentages', 'quant', 'hard',
      `A salary of $${orig} is increased by 20% and then decreased by 15%. What is the net percentage change in salary?`,
      ['2% increase', '2% decrease', '5% increase', '4% increase'],
      'a',
      `1. After 20% increase: $${orig} × 1.20 = $${afterInc}.\n2. After 15% decrease: $${afterInc} × 0.85 = $${netVal}.\n3. Net change multiplier = 1.20 × 0.85 = 1.02 (a 2% increase).`,
      'Multiply the successive change factors: 1.20 × 0.85 = 1.02.'
    );
  }
}

// 2. RATIOS
function genRatios(id, difficulty, index) {
  if (difficulty === 'easy') {
    const a = (index % 5) + 1;
    const b = a * 2;
    const total = (a + b) * 5;
    const shareA = (a / (a + b)) * total;
    const shareB = (b / (a + b)) * total;
    return makeQ(id, 'ratios', 'quant', 'easy',
      `A total of $${total} is divided between Alice and Bob in the ratio ${a}:${b}. How much does Alice receive?`,
      [`$${shareA - 10}`, `$${shareA}`, `$${shareB}`, `$${total - 5}`],
      'b',
      `Total parts = ${a} + ${b} = ${a + b}.\nValue per part = $${total} / ${a + b} = $${total / (a + b)}.\nAlice's share = ${a} × $${total / (a + b)} = $${shareA}.`,
      `Alice's share = (${a} / ${a + b}) × $${total}.`
    );
  } else if (difficulty === 'medium') {
    const total = index * 100 + 200;
    const shareC = (5 / 10) * total;
    return makeQ(id, 'ratios', 'quant', 'medium',
      `An amount of $${total} is distributed among A, B, and C in the ratio 2:3:5. What is C's share?`,
      [`$${shareC - 20}`, `$${shareC}`, `$${shareC + 20}`, `$${shareC + 50}`],
      'b',
      `Total ratio parts = 2 + 3 + 5 = 10.\nC's fraction = 5 / 10 = 1/2.\nC's share = $${total} / 2 = $${shareC}.`,
      "C gets 5 out of 10 equal parts of the total sum."
    );
  } else {
    const m1 = index * 2 + 10;
    const m2 = index * 3 + 15;
    const diff = m1 - m2;
    return makeQ(id, 'ratios', 'quant', 'hard',
      `In a mixture of ${m1 + m2} liters, the ratio of milk to water is 3:2. How many liters of water must be added to make the ratio of milk to water 1:1?`,
      [`${diff} liters`, `${diff + 2} liters`, `${m1} liters`, `${m2} liters`],
      'a',
      `Milk = 3/5 × ${m1 + m2} = ${m1} liters.\nWater = 2/5 × ${m1 + m2} = ${m2} liters.\nTo make ratio 1:1, Water must equal Milk (${m1} liters).\nWater to add = ${m1} - ${m2} = ${diff} liters.`,
      "Calculate the initial quantity of milk and water, then find the additional water needed to equal milk."
    );
  }
}

// 3. AVERAGES
function genAverages(id, difficulty, index) {
  if (difficulty === 'easy') {
    const num1 = index * 5 + 10;
    const num2 = index * 5 + 20;
    const num3 = index * 5 + 30;
    const avg = (num1 + num2 + num3) / 3;
    return makeQ(id, 'averages', 'quant', 'easy',
      `What is the average of ${num1}, ${num2}, and ${num3}?`,
      [`${avg - 5}`, `${avg}`, `${avg + 5}`, `${avg + 10}`],
      'b',
      `Average = (${num1} + ${num2} + ${num3}) / 3 = ${num1 + num2 + num3} / 3 = ${avg}.`,
      "Add all numbers together and divide by 3."
    );
  } else if (difficulty === 'medium') {
    const count = 5;
    const initialAvg = index * 2 + 50;
    const newNum = initialAvg + 12;
    const newAvg = (initialAvg * count + newNum) / (count + 1);
    return makeQ(id, 'averages', 'quant', 'medium',
      `The average score of 5 students in a test is ${initialAvg}. If a 6th student scores ${newNum}, what is the new average score of all 6 students?`,
      [`${newAvg - 2}`, `${newAvg}`, `${newAvg + 2}`, `${newAvg + 4}`],
      'b',
      `Initial total score = 5 × ${initialAvg} = ${5 * initialAvg}.\nNew total score = ${5 * initialAvg} + ${newNum} = ${5 * initialAvg + newNum}.\nNew average = ${5 * initialAvg + newNum} / 6 = ${newAvg}.`,
      "Find total score of first 5 students, add 6th score, then divide by 6."
    );
  } else {
    return makeQ(id, 'averages', 'quant', 'hard',
      `A motorist travels from Town A to Town B at a speed of 40 km/h and returns along the same route at 60 km/h. What is the average speed for the entire round trip?`,
      ['50 km/h', '48 km/h', '45 km/h', '52 km/h'],
      'b',
      `Formula: Average Speed = (2 × s1 × s2) / (s1 + s2) = (2 × 40 × 60) / (40 + 60) = 4800 / 100 = 48 km/h.`,
      "Use harmonic mean formula 2s1s2 / (s1 + s2) when equal distances are covered at different speeds."
    );
  }
}

// 4. PROFIT & LOSS
function genProfitLoss(id, difficulty, index) {
  if (difficulty === 'easy') {
    const cp = index * 50 + 100;
    const sp = cp + 50;
    const profitPct = ((sp - cp) / cp) * 100;
    return makeQ(id, 'profit_loss', 'quant', 'easy',
      `A merchant buys an item for $${cp} and sells it for $${sp}. What is the profit percentage?`,
      [`${profitPct.toFixed(1)}%`, `${(profitPct + 5).toFixed(1)}%`, `${(profitPct - 2).toFixed(1)}%`, `${(profitPct + 10).toFixed(1)}%`],
      'a',
      `Profit = $${sp} - $${cp} = $${sp - cp}.\nProfit % = (Profit / Cost Price) × 100 = (${sp - cp} / ${cp}) × 100 = ${profitPct.toFixed(1)}%.`,
      "Profit % = [(SP - CP) / CP] × 100."
    );
  } else if (difficulty === 'medium') {
    const cp = index * 40 + 200;
    const sp = cp * 0.90;
    return makeQ(id, 'profit_loss', 'quant', 'medium',
      `An article bought for $${cp} is sold at a loss of 10%. What is the selling price?`,
      [`$${sp - 10}`, `$${sp}`, `$${sp + 10}`, `$${sp + 20}`],
      'b',
      `Loss = 10% of $${cp} = $${cp * 0.10}.\nSelling Price = $${cp} - $${cp * 0.10} = $${sp}.`,
      "Selling Price = CP × (1 - Loss%/100)."
    );
  } else {
    const mp = index * 50 + 500;
    const sp = mp * 0.80;
    const cp = sp / 1.25;
    return makeQ(id, 'profit_loss', 'quant', 'hard',
      `A shopkeeper marks an item at $${mp}. After offering a 20% discount on the marked price, he still makes a 25% profit. What was the cost price of the item?`,
      [`$${cp - 20}`, `$${cp}`, `$${cp + 20}`, `$${cp + 40}`],
      'b',
      `1. Selling Price = Marked Price × 0.80 = $${mp} × 0.80 = $${sp}.\n2. Cost Price = Selling Price / 1.25 = $${sp} / 1.25 = $${cp}.`,
      "First find SP using discount on Marked Price, then calculate CP from profit percentage."
    );
  }
}

// 5. TIME & WORK
function genTimeWork(id, difficulty, index) {
  if (difficulty === 'easy') {
    return makeQ(id, 'time_work', 'quant', 'easy',
      `A can complete a task in 10 days and B can complete the same task in 15 days. How many days will they take working together?`,
      ['5 days', '6 days', '7.5 days', '8 days'],
      'b',
      `A's 1-day work = 1/10.\nB's 1-day work = 1/15.\nCombined 1-day work = 1/10 + 1/15 = (3 + 2)/30 = 5/30 = 1/6.\nTotal days required = 6 days.`,
      "Combined time = (A × B) / (A + B)."
    );
  } else if (difficulty === 'medium') {
    return makeQ(id, 'time_work', 'quant', 'medium',
      `Worker A finishes a project in 12 days. Worker B finishes it in 24 days. Working together, what fraction of the project do they finish in 2 days?`,
      ['1/8', '1/4', '1/3', '1/2'],
      'b',
      `Combined 1-day work = 1/12 + 1/24 = 3/24 = 1/8.\nIn 2 days, combined work done = 2 × (1/8) = 2/8 = 1/4.`,
      "Find combined 1-day work rate and multiply by 2."
    );
  } else {
    return makeQ(id, 'time_work', 'quant', 'hard',
      `Pipe A can fill a tank in 12 hours and Pipe B can fill it in 15 hours. Pipe C can empty the full tank in 20 hours. If all three pipes are opened simultaneously, how long will it take to fill the tank?`,
      ['8 hours', '10 hours', '12 hours', '15 hours'],
      'b',
      `Rate of Pipe A = +1/12.\nRate of Pipe B = +1/15.\nRate of Pipe C = -1/20.\nNet filling rate = 1/12 + 1/15 - 1/20 = (5 + 4 - 3) / 60 = 6/60 = 1/10 per hour.\nTime to fill tank = 10 hours.`,
      "Add filling rates and subtract emptying rate to get net hourly filling rate."
    );
  }
}

// 6. NUMBER SERIES
function genNumberSeries(id, difficulty, index) {
  if (difficulty === 'easy') {
    const start = index * 3 + 2;
    const step = 4;
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
    return makeQ(id, 'number_series', 'logical', 'medium',
      `Identify the missing term in the sequence: 2, 6, 12, 20, ?`,
      ['28', '30', '32', '36'],
      'b',
      `Pattern: 1×2=2, 2×3=6, 3×4=12, 4×5=20, 5×6=30.\nAlternatively, differences increase by +2 (+4, +6, +8, +10). 20 + 10 = 30.`,
      "Look at consecutive differences: +4, +6, +8... next is +10."
    );
  } else {
    return makeQ(id, 'number_series', 'logical', 'hard',
      `Find the next number in the series: 3, 5, 9, 17, 33, ?`,
      ['65', '63', '67', '70'],
      'a',
      `Pattern: Differences are powers of 2 (2, 4, 8, 16, 32).\n3 + 2 = 5\n5 + 4 = 9\n9 + 8 = 17\n17 + 16 = 33\n33 + 32 = 65.`,
      "Analyze the differences between terms: 2, 4, 8, 16... each difference doubles."
    );
  }
}

// 7. DIRECTIONS
function genDirections(id, difficulty, index) {
  if (difficulty === 'easy') {
    return makeQ(id, 'directions', 'logical', 'easy',
      `A person walks 5 km North, then turns Right and walks 5 km. In which direction is the person relative to the starting point?`,
      ['North', 'North-East', 'East', 'South-East'],
      'b',
      `1. Walking 5 km North moves along the +Y axis.\n2. Turning Right moves East along the +X axis.\n3. The resultant position has positive North and East displacement, which is North-East.`,
      "Draw the movement on a compass grid: North + East = North-East."
    );
  } else if (difficulty === 'medium') {
    return makeQ(id, 'directions', 'logical', 'medium',
      `A cyclist travels 12 km East, turns North and travels 5 km. What is the shortest straight-line distance from the starting point?`,
      ['13 km', '15 km', '17 km', '10 km'],
      'a',
      `The path forms a right-angled triangle with legs 12 km and 5 km.\nBy Pythagoras Theorem: Distance = √(12² + 5²) = √(144 + 25) = √169 = 13 km.`,
      "Use Pythagoras Theorem: hypotenuse = √(base² + height²)."
    );
  } else {
    return makeQ(id, 'directions', 'logical', 'hard',
      `At 3:00 PM, the minute hand of a clock points towards North-East. In which direction does the hour hand point?`,
      ['South-East', 'South-West', 'North-West', 'North'],
      'a',
      `At 3:00 PM, standard clock positions are: Minute hand at 12 (North) and Hour hand at 3 (East).\nIf 12 is rotated 45° clockwise to North-East, then 3 (East) is rotated 45° clockwise to South-East.`,
      "Observe that hands are 90° apart clockwise at 3:00 PM. 90° clockwise from North-East is South-East."
    );
  }
}

// 8. SYLLOGISMS
function genSyllogisms(id, difficulty, index) {
  if (difficulty === 'easy') {
    return makeQ(id, 'syllogisms', 'logical', 'easy',
      `Statements: All cats are mammals. All mammals are animals.\nConclusions:\nI. All cats are animals.\nII. Some animals are cats.`,
      ['Only conclusion I follows', 'Only conclusion II follows', 'Both conclusions I and II follow', 'Neither conclusion follows'],
      'c',
      `1. All Cats ⊂ Mammals ⊂ Animals => All Cats are Animals (I is valid).\n2. Since Cats exist inside Animals, at least Some Animals are Cats (II is valid).\nTherefore, both conclusions I and II follow.`,
      "Use nested circles (Cats inside Mammals inside Animals) to test validity."
    );
  } else if (difficulty === 'medium') {
    return makeQ(id, 'syllogisms', 'logical', 'medium',
      `Statements: Some apples are fruits. No fruits are vegetables.\nConclusions:\nI. Some apples are not vegetables.\nII. All vegetables are apples.`,
      ['Only conclusion I follows', 'Only conclusion II follows', 'Both follow', 'Neither follows'],
      'a',
      `1. The apples that are fruits CANNOT be vegetables because No fruits are vegetables. Thus, those apples are not vegetables (I follows).\n2. Vegetables and apples have no forced universal inclusion (II does not follow).`,
      "Apples that overlap with fruits can never overlap with vegetables."
    );
  } else {
    return makeQ(id, 'syllogisms', 'logical', 'hard',
      `Statements: No A is B. No B is C.\nConclusions:\nI. No A is C.\nII. Some A are C.`,
      ['Only conclusion I follows', 'Only conclusion II follows', 'Either conclusion I or II follows', 'Neither conclusion follows'],
      'c',
      `No direct relationship is established between A and C from the premises.\nHowever, A and C must either have no common elements (No A is C) or at least one common element (Some A are C).\nSince they form a complementary pair, Either I or II must follow.`,
      "Two statements with no link between A and C form an 'Either/Or' complementary pair for 'No A is C' and 'Some A are C'."
    );
  }
}

// 9. GRAMMAR & VOCABULARY
function genGrammarVocab(id, difficulty, index) {
  if (difficulty === 'easy') {
    const syns = [
      { word: 'CANDID', ans: 'Frank', options: ['Deceitful', 'Frank', 'Secretive', 'Cautious'], exp: "'Candid' means truthful, straightforward, or frank in expression." },
      { word: 'BENEVOLENT', ans: 'Kind', options: ['Malevolent', 'Kind', 'Greedy', 'Hostile'], exp: "'Benevolent' means well-meaning and kindly." },
      { word: 'DILIGENT', ans: 'Hardworking', options: ['Lazy', 'Hardworking', 'Careless', 'Indifferent'], exp: "'Diligent' means showing care and effort in work." },
      { word: 'METICULOUS', ans: 'Precise', options: ['Sloppy', 'Precise', 'Hasty', 'Rough'], exp: "'Meticulous' means showing great attention to detail." }
    ];
    const item = syns[(index - 1) % syns.length];
    return makeQ(id, 'grammar_vocab', 'verbal', 'easy',
      `Choose the synonym of '${item.word}':`,
      item.options,
      'b',
      item.exp,
      "Select the option closest in meaning."
    );
  } else if (difficulty === 'medium') {
    return makeQ(id, 'grammar_vocab', 'verbal', 'medium',
      `Identify the correct sentence with proper Subject-Verb Agreement:`,
      ['The committee have decided its final verdict.', 'The list of items are on the table.', 'Neither of the candidates has answered.', 'Every student and teacher were present.'],
      'c',
      `'Neither' takes a singular verb ('has'). Incorrect options: 'list' is singular so it requires 'is', 'committee' as a single body takes singular 'has'.`,
      "Indefinite pronouns like 'neither', 'either', and 'each' require singular verbs."
    );
  } else {
    return makeQ(id, 'grammar_vocab', 'verbal', 'hard',
      `Choose the word most OPPOSITE in meaning to 'EPHEMERAL':`,
      ['Transient', 'Perpetual', 'Fleeting', 'Evanescent'],
      'b',
      `'Ephemeral' means lasting for a very short time. Its opposite is 'Perpetual', which means lasting forever.`,
      "'Ephemeral' means temporary; look for a word that means everlasting."
    );
  }
}

// 10. READING COMPREHENSION
function genReadingComp(id, difficulty, index) {
  if (difficulty === 'easy') {
    return makeQ(id, 'reading_comp', 'verbal', 'easy',
      `Passage: 'Renewable energy adoption has accelerated globally due to declining solar panel costs and urgent climate policies.'\nQuestion: According to the passage, what is a primary driver of renewable energy adoption?`,
      ['Increased fossil fuel subsidies', 'Declining solar panel costs', 'Strict nuclear energy bans', 'Reduced energy consumption'],
      'b',
      `The passage directly states that 'declining solar panel costs' accelerated adoption.`,
      "Locate the direct cause mentioned in the sentence."
    );
  } else if (difficulty === 'medium') {
    return makeQ(id, 'reading_comp', 'verbal', 'medium',
      `Passage: 'While automation increases manufacturing throughput, critics argue it displaces entry-level workers without immediately creating equivalent technical roles.'\nQuestion: What is the main concern raised by critics?`,
      ['Reduced product quality', 'Displacement of entry-level workers', 'Excessive technical training costs', 'Decreased manufacturing speed'],
      'b',
      `Critics specifically argue that automation 'displaces entry-level workers' before technical jobs emerge.`,
      "Focus on what critics in the passage argue against automation."
    );
  } else {
    return makeQ(id, 'reading_comp', 'verbal', 'hard',
      `Passage: 'Scientific consensus indicates that biodiversity loss weakens ecosystem resilience, making habitats increasingly vulnerable to extreme climate events.'\nQuestion: Which statement best expresses the primary inference of the passage?`,
      ['Ecosystems can adapt effortlessly to climate events.', 'Preserving biodiversity is crucial for ecosystem stability against climate shocks.', 'Climate events are the sole cause of biodiversity loss.', 'Biodiversity loss only affects marine habitats.'],
      'b',
      `The passage links biodiversity to resilience against climate events; preserving biodiversity therefore protects ecosystem stability.`,
      "Derive the logical recommendation supported by the link between biodiversity and resilience."
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
