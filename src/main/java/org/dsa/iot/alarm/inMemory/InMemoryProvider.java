/* THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD
 * TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN
 * NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER
 * IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN
 * CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package org.dsa.iot.alarm.inMemory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import org.dsa.iot.alarm.AbstractProvider;
import org.dsa.iot.alarm.AlarmClass;
import org.dsa.iot.alarm.AlarmCursor;
import org.dsa.iot.alarm.AlarmRecord;
import org.dsa.iot.alarm.Note;
import org.dsa.iot.alarm.NoteCursor;

/**
 * Alarming provider that stores alarms in memory using Java collections.
 *
 * @author Aaron Hansen
 */
@SuppressFBWarnings("IS2_INCONSISTENT_SYNC")
public class InMemoryProvider extends AbstractProvider {

    ///////////////////////////////////////////////////////////////////////////
    // Constants
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Fields
    ///////////////////////////////////////////////////////////////////////////

    private static Comparator<AlarmRecord> alarmComparator = new AlarmComparator();
    private Set<AlarmRecord> alarmCache = null;
    private Map<UUID, AlarmRecord> alarmMap = new TreeMap<>();
    private Set<AlarmRecord> alarmSet = new TreeSet<>(alarmComparator);
    private List<Note> noteCache = null;
    private List<Note> noteList = new LinkedList<>();

    ///////////////////////////////////////////////////////////////////////////
    // Constructors
    ///////////////////////////////////////////////////////////////////////////

    public InMemoryProvider() {
    }

    ///////////////////////////////////////////////////////////////////////////
    // Methods
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public synchronized void addAlarm(AlarmRecord record) {
        alarmSet.add(record);
        alarmMap.put(record.getUuid(), record);
        alarmCache = null;
    }

    @Override
    protected synchronized void addNote(Note note) {
        noteList.add(note);
        noteCache = null;
    }

    @Override
    public void deleteAllRecords() {
        alarmSet.clear();
        alarmMap.clear();
        noteList.clear();
        alarmCache = null;
        noteCache = null;
    }

    @Override
    public synchronized void deleteRecord(UUID uuid) {
        AlarmRecord record = alarmMap.remove(uuid);
        if (record == null) {
            return;
        }
        alarmSet.remove(record);
        for (Iterator<Note> it = noteList.iterator(); it.hasNext(); ) {
            if (it.next().getUUID().equals(uuid)) {
                it.remove();
            }
        }
        alarmCache = null;
        noteCache = null;
    }

    @Override
    public synchronized AlarmRecord getAlarm(UUID uuid) {
        return alarmMap.get(uuid);
    }

    /**
     * Returns an iterator on the cache, creating the cache if necessary.
     */
    private synchronized Iterator<AlarmRecord> getAlarmIterator() {
        if (alarmCache == null) {
            alarmCache = new TreeSet<>(alarmComparator);
            alarmCache.addAll(alarmSet);
        }
        return alarmCache.iterator();
    }

    /**
     * Returns an iterator on the cache, creating the cache if necessary.
     */
    private synchronized Iterator<Note> getNoteIterator() {
        if (noteCache == null) {
            noteCache = new ArrayList<>();
            noteCache.addAll(noteList);
        }
        return noteCache.iterator();
    }

    @Override
    public NoteCursor getNotes(UUID uuid) {
        return new MyNoteCursor(uuid);
    }

    @Override
    protected void saveRecord(AlarmRecord arg) {
        AlarmRecord rec = getAlarm(arg.getUuid());
        if (rec != arg) {
            rec.copy(arg);
        }
    }

    @Override
    public AlarmCursor queryAlarms(AlarmClass alarmClass, Calendar from,
                                   Calendar to) {
        return new MyAlarmCursor(alarmClass, false, from, to);
    }

    @Override
    public AlarmCursor queryOpenAlarms(AlarmClass alarmClass) {
        return new MyAlarmCursor(alarmClass, true, null, null);
    }

    ///////////////////////////////////////////////////////////////////////////
    // Inner Classes
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Sorts by created time, only returns 0 when UUIDs are equal.
     */
    @SuppressFBWarnings("SE_COMPARATOR_SHOULD_BE_SERIALIZABLE")
    private static class AlarmComparator implements Comparator<AlarmRecord> {

        public AlarmComparator() {
        }

        public int compare(AlarmRecord r1, AlarmRecord r2) {
            if (r1 == r2) {
                return 0;
            }
            if (r1.getUuid().equals(r2.getUuid())) {
                return 0;
            }
            if (r1.getCreatedTime() < r2.getCreatedTime()) {
                return -1;
            }
            return 1;
        }
    } //AlarmComparator

    /**
     * Uses an iterator on the noteCache.
     */
    private class MyAlarmCursor extends AlarmCursor {

        private AlarmClass alarmClass;
        private long from = Long.MIN_VALUE;
        private Iterator<AlarmRecord> iterator;
        private boolean openOnly = false;
        private long to = Long.MAX_VALUE;

        MyAlarmCursor(AlarmClass alarmClass, boolean open, Calendar from, Calendar to) {
            iterator = getAlarmIterator();
            this.alarmClass = alarmClass;
            this.openOnly = open;
            if (from != null) {
                this.from = from.getTimeInMillis();
            }
            if (to != null) {
                this.to = to.getTimeInMillis();
            }
        }

        @Override
        public void close() {
        }

        @Override
        public boolean next() {
            if (iterator == null) {
                return false;
            }
            AlarmRecord tmp;
            long ts;
            while (iterator.hasNext()) {
                tmp = iterator.next();
                if (openOnly && tmp.isClosed()) {
                    continue;
                }
                if ((alarmClass == null) || (alarmClass == tmp.getAlarmClass())) {
                    ts = tmp.getCreatedTime();
                    if (ts < from) {
                        continue;
                    }
                    if (ts >= to) {
                        break;
                    }
                    copy(tmp);
                    return true;
                }
            }
            iterator = null;
            return false;
        }

    }//MyAlarmCursor

    /**
     * Uses an iterator on the noteCache.
     */
    private class MyNoteCursor extends NoteCursor {

        private Iterator<Note> iterator;
        private UUID uuid;

        MyNoteCursor(UUID uuid) {
            this.uuid = uuid;
            iterator = getNoteIterator();
        }

        @Override
        public void close() {
        }

        @Override
        public boolean next() {
            Note tmp;
            while (iterator.hasNext()) {
                tmp = iterator.next();
                if (tmp.getUUID().equals(uuid)) {
                    copy(tmp);
                    return true;
                }
            }
            return false;
        }

    }//MyNoteCursor

} //class
