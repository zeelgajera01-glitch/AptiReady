"""Run with Python 3 from any directory. Exercises the actual DAO SQL; does not replace Android tests."""
from pathlib import Path
import re
import sqlite3

root = Path(__file__).resolve().parents[1]
base = root / 'app/src/main/java/com/example/aptiready/data/local/db'
source = (base / 'PracticeSessionDao.kt').read_text()
query = re.search(r'@Query\("""(.*?)"""\)\s*suspend fun unlockExtraHintOwned', source, re.S).group(1)
connection = sqlite3.connect(':memory:')
connection.executescript('''
CREATE TABLE practice_sessions(id TEXT PRIMARY KEY, ownerId TEXT);
CREATE TABLE session_question_snapshots(sessionId TEXT, position INTEGER, questionId TEXT,
    extraHint TEXT, isExtraHintUnlocked INTEGER, selectedOptionId TEXT);
INSERT INTO practice_sessions VALUES ('s1', 'alice');
INSERT INTO session_question_snapshots VALUES ('s1', 0, 'q1', 'Valid question-specific hint', 0, 'b');
INSERT INTO session_question_snapshots VALUES ('s1', 1, 'q2', '', 0, 'a');
''')
args = dict(sessionId='s1', position=0, questionId='q1', ownerId='bob')
assert connection.execute(query, args).rowcount == 0, 'Wrong owner received reward'
args['ownerId'] = 'alice'
assert connection.execute(query, args).rowcount == 1, 'Valid earned hint not unlocked'
assert connection.execute(query, args).rowcount == 0, 'Duplicate callback updated entitlement'
assert connection.execute('SELECT selectedOptionId FROM session_question_snapshots WHERE position=0').fetchone()[0] == 'b'
args.update(position=1, questionId='q2')
assert connection.execute(query, args).rowcount == 0, 'Missing content was unlocked'
source = (base / 'AppDatabase.kt').read_text()
invalid = re.search(r'val invalid = "([^"]+)"', source).group(1)
for table in ('questions', 'session_question_snapshots', 'bookmarks'):
    db = sqlite3.connect(':memory:')
    db.execute(f'CREATE TABLE {table}(extraHint TEXT, preserved TEXT)')
    db.executemany(f'INSERT INTO {table} VALUES (?,?)', [(invalid, 'history'), ('Valid hint', 'answer')])
    db.execute(f"UPDATE `{table}` SET `extraHint` = '' WHERE `extraHint` = ?", (invalid,))
    assert db.execute(f'SELECT * FROM {table}').fetchall() == [('', 'history'), ('Valid hint', 'answer')]
    db.close()
print('PASS: owner isolation, idempotent grant, preserved answer, absent hint and three-table cleanup.')
