import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveAction;

class ParallelMergeSort extends RecursiveAction {
	private static final long serialVersionUID = 4906469437150445110L;
	private final int low, high, array[];

	ParallelMergeSort(final int[] array, final int low, final int high) {
		this.array = array;
		this.low = low;
		this.high = high;
	}

	void merge(int arr[], int l, int m, int r) {
		int n1 = m - l + 1;
		int n2 = r - m;
		int L[] = new int[n1];
		int R[] = new int[n2];
		for (int i = 0; i < n1; ++i)
			L[i] = arr[l + i];
		for (int j = 0; j < n2; ++j)
			R[j] = arr[m + 1 + j];
		int i = 0, j = 0;
		int k = l;
		while (i < n1 && j < n2)
			if (L[i] <= R[j])
				arr[k++] = L[i++];
			else
				arr[k++] = R[j++];

		while (i < n1)
			arr[k++] = L[i++];
		while (j < n2)
			arr[k++] = R[j++];
	}

	@Override
	protected void compute() {
		if (low < high) {
			final int middle = (low + high) / 2;
			final ParallelMergeSort left = new ParallelMergeSort(array, low, middle);
			final ParallelMergeSort right = new ParallelMergeSort(array, middle + 1, high);
			invokeAll(left, right);
			merge(array, low, middle, high);
		}
	}
}

public class Main {
	static class Triple {
		long sum = 0;
		int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;

		@Override
		public String toString() {
			return "Summation: " + sum + ", Minimum: " + min + ", Maximum: " + max;
		}
	}

	static int[] array = new int[(int) 100];

	public static void parallelAssignment() {
		for (int i = 0; i < array.length; i++)
			array[i] = new Random().nextInt(1000);
	}
	
	public static void main(String[] args) throws Exception {
		parallelAssignment();
		System.out.println("Array before sort: " + Arrays.toString(array));
		long curTime = System.currentTimeMillis();
		final ForkJoinPool forkJoinPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors() - 1);
		forkJoinPool.invoke(new ParallelMergeSort(array, 0, array.length - 1));
		System.out.println("Parallel merge sort execution time: " + (System.currentTimeMillis() - curTime)+" Milliseconds");
		System.out.println("Array after sort: " + Arrays.toString(array));
		int cpus = Runtime.getRuntime().availableProcessors();
		ExecutorService service = Executors.newFixedThreadPool(cpus);
		List<Future<Triple>> tasks = new ArrayList<>();
		int blockSize = (array.length + cpus - 1) / cpus;
		for (int i = 0; i < cpus; i++) {
			final int start = blockSize * i;
			final int end = (int) Math.min((long) blockSize * (i + 1), array.length);
			tasks.add(service.submit(() -> {
				Triple tri = new Triple();
				for (int j = start; j < end; j++) {
					tri.sum += array[j];
					tri.min = Math.min(tri.min, array[j]);
					tri.max = Math.max(tri.max, array[j]);
				}
				return tri;
			}));
		}
		Triple result = new Triple();
		curTime = System.currentTimeMillis();
		for (Future<Triple> task : tasks) {
			result.sum += task.get().sum;
			result.min = Math.min(result.min, task.get().min);
			result.max = Math.max(result.max, task.get().max);
		}
		System.out.println("Parallel summation, minimization and maximization execution time: "
				+ (System.currentTimeMillis() - curTime));
		System.out.println(result);
		service.shutdown();
	}
}
