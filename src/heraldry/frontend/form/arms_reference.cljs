(ns heraldry.frontend.form.arms-reference
  (:require [heraldry.frontend.form.element :as element]))

(defn form [db-path]
  (let [[x y] (last db-path)]
    [element/component db-path :arms-reference (str x ", " y) nil]))
