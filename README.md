# Bogus

[NIH]: https://en.wikipedia.org/wiki/Not_invented_here

A small, GUI-powered, [NIH][NIH]-reasoned debugger for Clojure.

## Installation

Lein:

```clojure
[com.github.igrishaev/bogus "0.1.0"]
```

Deps.edn

```clojure
{com.github.igrishaev/bofus {:mvn/version "0.1.0"}}
```

The best way to use Bogus is to setup it locally in your `profiles.clj` file:

```clojure
;; ~/.lein/profiles.clj

{:user
 {:dependencies [[com.github.igrishaev/bogus "0.1.0"]]
  :injections [(require 'bogus.core)]}}
```

## Usage

Once you have the dependency and the `bogus.core` namespace imported, place the
`#bg/debug` tag anywhere in your code:

```clojure
(defn do-some-action []
  (let [a 1
        b 2
        c (+ a b)]
    #bg/debug
    (+ a b c)))
```

Now run the function, and you'll see the UI:

![](img/screen1.png)

(to be continued)

Copyright &copy; 2022 Ivan Grishaev
