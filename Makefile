
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

toc-install:
	npm install --save markdown-toc

toc-build:
	node_modules/.bin/markdown-toc -i README.md
