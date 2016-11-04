public class Test1 implements TestClass  {
    private MyInt myInt;

    public Test1() throws IndexOutOfBoundsException {
        throw new IndexOutOfBoundsException("Hello");
    }

    public void setUp() {
        myInt=new MyInt();
    }

    public void tearDown() {
        myInt=null;
    }

    //Test that should succeed
    public boolean testInitialisation() {
        return myInt.value()==0;
    }

    //Test that should succeed
    public boolean testIncrement() {
        myInt.increment();
        myInt.increment();
        return myInt.value()==2;

    }

    //Test that should succeed
    public boolean testDecrement() {
        myInt.increment();
        myInt.decrement();
        return myInt.value()==0;
    }

    //Test that should fail
    public boolean testFailingByException() {
        myInt=null;
        myInt.decrement();
        return true;

    }

    //Test that should fail
    public boolean testFailing() {
        return false;

    }

    public boolean testSleep() throws InterruptedException {
        Thread.sleep(3000);
        return true;
    }
}