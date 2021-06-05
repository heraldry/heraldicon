(ns heraldry.frontend.form.collection
  (:require [heraldry.frontend.form.arms-reference :as arms-reference]
            [heraldry.frontend.form.element :as element]
            [heraldry.frontend.form.font :as font]
            [re-frame.core :as rf]))

(defn collection-element [path]
  (let [index (last path)
        data @(rf/subscribe [:get path])
        element-name (-> data
                         :name
                         (or "None"))]
    [element/component path :collection-element (str (inc index) ": " element-name) nil
     [:div.setting {:style {:margin-bottom "1em"}}
      [element/text-field (conj path :name) "Name" :style {:width "19em"}]]

     [arms-reference/form (conj path :reference)]]))

(defn form [path selected-arms-path]
  (let [data @(rf/subscribe [:get path])
        selected-arms @(rf/subscribe [:get selected-arms-path])]
    [element/component path :collection (:name data) "Collection"
     [:div.setting {:style {:margin-bottom "1em"}}
      [element/text-field (conj path :name) "Name" :style {:width "19em"}]]
     [:div.setting {:style {:margin-bottom "1em"}}
      [font/form (conj path :font)]]

     [:div.settings
      [element/range-input (conj path :collection :num-columns) "Columns"
       1
       10
       :default 6]]

     (when selected-arms
       [:div.selected-arms
        [collection-element (conj path :collection :elements selected-arms)]])]))
