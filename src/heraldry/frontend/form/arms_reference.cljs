(ns heraldry.frontend.form.arms-reference
  (:require [heraldry.frontend.form.element :as element]
            [re-frame.core :as rf]))

(defn form [db-path]
  (let [index (last db-path)
        data @(rf/subscribe [:get db-path])
        title (-> data
                  :name
                  (or "None"))]
    [element/component db-path :arms-reference (str (inc index) ": " title) nil
     [:div {:style {:margin-bottom "1em"}}
      [element/text-field (conj db-path :name) "Name" :style {:width "19em"}]]]))
