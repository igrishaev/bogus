
all:
	lein install


.PHONY: test
test:
	lein test


.PHONY: release
release:
	lein release
