/*
 * File: TestFormat.java
 * Author: Fredrik Johansson
 * Date: 2016-10-31
 */

public class TestFormat implements TestClass {

    public void testShouldNotRunIncorrectReturnType() { }

    public boolean testShouldNotRunHasParameter(int i) {
        return true;
    }

    public boolean shouldNotRunDontStartWithTest() {
        return true;
    }

    protected boolean testShouldNotRunNotPublic() {
        return true;
    }

    public boolean testThisShouldBeTheOnlyTestToRun() {
        return true;
    }
}
