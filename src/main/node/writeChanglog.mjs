import db from './db/index.js';
import * as fs from 'node:fs/promises';
import dayjs from "dayjs";

/**
 * This script will dump the contents of the change log table out to the "data.txt" file in this repo. This should
 * document how the CPIC data has changed over time and also trigger a change to this repo whenever underlying data
 * changes.
 */
try {
  const changes = await db.many('select date, type, entityname, note from change_log_view order by date desc, type, entityname, note');

  let content = 'Date of Change\tType of Data\tSubject\tNote of Change\n';
  for (let i = 0; i < changes.length; i++) {
    const change = changes[i];
    const line = [dayjs(change.date).format('YYYY-MM-DD'), change.type, change.entityname, change.note.replaceAll('\n', '\n\t\t\t')];
    content += line.join('\t') + '\n';
  }

  await fs.writeFile('../../../data.txt', content);
  process.exit(0);
} catch (err) {
  console.error(err);
  process.exit(1);
}
