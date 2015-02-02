package main;

public class Main {

	public static void main(String[] args) {

		Receiver r = new Receiver();

		try {

			synchronized (r.streamLock) {

				r.streamLock.wait();
			}

			Inspector i = new Inspector();

		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("-=-==-=-=-=-=-=-- MAIN ENDED");
	}

}
