
all: install

install:
	lein install

.PHONY: test
test:
	lein test

release:
	lein release

repl:
	lein repl
