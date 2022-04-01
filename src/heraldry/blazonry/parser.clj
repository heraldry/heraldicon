(ns heraldry.blazonry.parser
  (:require
   [shadow.resource :as res]))

(defmacro load-grammar-template []
  (res/slurp-resource &env "grammar.ebnf"))
