(ns heraldry.frontend.form.collection
  (:require [heraldry.frontend.form.arms-reference :as arms-reference]
            [heraldry.frontend.form.element :as element]
            [re-frame.core :as rf]))

(defn form [path selected-arms-path]
  (let [data @(rf/subscribe [:get path])
        selected-arms @(rf/subscribe [:get selected-arms-path])]
    [element/component path :collection (:name data) "Collection"
     [:div.settings
      [element/range-input (conj path :collection :num-columns) "Columns"
       1
       10
       :default 6]
      [element/range-input (conj path :collection :num-rows) "Rows"
       1
       30
       :default 10]]

     (when selected-arms
       [:div.selected-arms
        [arms-reference/form (conj path :arms selected-arms)]])]))
