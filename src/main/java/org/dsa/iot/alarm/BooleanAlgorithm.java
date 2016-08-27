/* THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD
 * TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN
 * NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER
 * IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN
 * CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package org.dsa.iot.alarm;

import org.dsa.iot.dslink.node.*;
import org.dsa.iot.dslink.node.value.*;

/**
 * This algorithm creates alarms when boolean data sources turn true.  This will allow
 * other links the self detect alarmable conditions.
 *
 * @author Aaron Hansen
 */
public class BooleanAlgorithm extends AlarmAlgorithm implements Runnable {

    ///////////////////////////////////////////////////////////////////////////
    // Constants
    ///////////////////////////////////////////////////////////////////////////

    private static final String ALARM_VALUE = "Alarm Value";

    ///////////////////////////////////////////////////////////////////////////
    // Fields
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Constructors
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////
    // Methods
    ///////////////////////////////////////////////////////////////////////////

    @Override protected String getAlarmMessage(AlarmWatch watch) {
        return "Value = " + watch.getCurrentValue().toString();
    }

    @Override protected void initData() {
        super.initData();
        initProperty(ALARM_VALUE, new Value(true)).setWritable(Writable.CONFIG);
    }

    @Override protected boolean isAlarm(AlarmWatch watch) {
        Value currentValue = watch.getCurrentValue();
        if (currentValue != null) {
            Boolean alarmValue = getProperty(ALARM_VALUE).getBool();
            Boolean curBool = currentValue.getBool();
            if (curBool != null) {
                return curBool.equals(alarmValue);
            }
            Number curNum = currentValue.getNumber();
            if (curNum != null) {
                if (alarmValue) {
                    return curNum.intValue() != 0;
                }
                return curNum.intValue() == 0;
            }
            String curStr = currentValue.getString();
            if (curStr != null) {
                if (alarmValue) {
                    return isTrue(curStr);
                }
                return isFalse(curStr);
            }
        }
        return false;
    }

    /**
     * Returns true if the arg is a well known string representation of false.
     */
    private boolean isFalse(String arg) {
        if (Boolean.FALSE.toString().equalsIgnoreCase(arg)) {
            return true;
        } else if ("0".equals(arg)) {
            return true;
        } else if ("off".equalsIgnoreCase(arg)) {
            return true;
        } else if ("inactive".equalsIgnoreCase(arg)) {
            return true;
        }
        return true;
    }

    /**
     * Returns true if the arg is a well known string representation of true.
     */
    private boolean isTrue(String arg) {
        if (Boolean.TRUE.toString().equalsIgnoreCase(arg)) {
            return true;
        } else if ("1".equals(arg)) {
            return true;
        } else if ("on".equalsIgnoreCase(arg)) {
            return true;
        } else if ("active".equalsIgnoreCase(arg)) {
            return true;
        }
        return false;
    }

    @Override protected void onPropertyChange(Node child, ValuePair valuePair) {
        if (ALARM_VALUE.equals(child.getName())) {
            AlarmUtil.enqueue(this);
        }
    }

}


