package bt7s7k7.picker_dollies.data;

import java.util.List;

public abstract class ScrollSelectedValue<T> {
	protected int selectedIdx = 0;

	public abstract List<T> getOptions();

	public abstract boolean canUse(T value);

	public void selectNext() {
		this.selectedIdx++;
		if (this.selectedIdx >= this.getOptions().size()) {
			this.selectedIdx = 0;
		}
	}

	public void selectPrevious() {
		this.selectedIdx--;
		if (this.selectedIdx < 0) {
			this.selectedIdx = this.getOptions().size() - 1;
		}
	}

	public ScrollSelectedValue<T> selectIfPossible(T value) {
		if (!this.canUse(value)) return this;

		while (this.get() != value) {
			this.selectNext();
		}

		return this;
	}

	public T get() {
		while (true) {
			// There should probably be a guard here against infinite loops, but there shouldn't be
			// a case where there are no activatable operations, because MoveOperation is always
			// activatable.

			var operation = this.getOptions().get(this.selectedIdx);
			if (this.canUse(operation)) return operation;
			this.selectNext();
		}
	}

}
