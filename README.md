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

Once you have the dependency added and the `bogus.core` namespace imported,
place one of these two forms into your code:

```clojure
(bogus.core/debug)
;; or
#bg/debug
```

For example:

```clojure
(defn do-some-action []
  (let [a 1
        b 2
        c (+ a b)]
    #bg/debug ;; or (bogus.core/debug)
    (+ a b c)))
```

Now run the function, and you'll see the UI:

![](img/screen1.png)

The UI window blocks execution of the code. In the example above, you'll hang
right before executing the next `(+ a b c)` form. Close the window to continue
execution of the code.

The UI consists from three parts: the input area, the output, and the log. Type
any Clojure-friendly form in the input textarea and press "Eval". The result
will take place in the output textarea. You can copy it from there to your
editor.

The input can take many Clojure forms at once. They are exceuted as follows:

```clojure
(eval '(do (form1) (form2 ...)))
```

so you'll get the result of the last one.

If you mark some text in the input with selection, only this fragment of code
will be execute.

![](img/screen2.png)

In your code, you can use any local variables as they're global ones. In the
example above we've executed the `(+ a b c)` form referencing local `a`, `b`,
and `c` from the `let` clause.

The "Locals" button pretty-prints the local variables. Bogus does it in advance
when the window opens the first time. The "Inspect" button opens the standard
`clojure.inspector/inspect-tree` widget rendering the locals. This is quite
useful when examining massive chunks of data.

![](img/screen3.png)

## How does it work

## Why

## Other

Copyright &copy; 2022 Ivan Grishaev
