(ns heraldry.frontend.form.attribution
  (:require [heraldry.frontend.form.element :as element]
            [re-frame.core :as rf]))

(defn form [db-path]
  (let [attribution-options [["None (No sharing)" :none]
                             ["CC Attribution" :cc-attribution]
                             ["CC Attribution-ShareAlike" :cc-attribution-share-alike]
                             ["Public Domain" :public-domain]]
        license-nature      @(rf/subscribe [:get (conj db-path :nature)])]
    [element/component db-path :attribution "Attribution / License" nil
     [element/select (conj db-path :license) "License" attribution-options]
     [element/radio-select (conj db-path :nature) [["Own work" :own-work]
                                                   ["Derivative" :derivative]]
      :default :own-work]
     (when (= license-nature :derivative)
       [:<>
        [element/select (conj db-path :source-license) "Source license" (assoc-in
                                                                         attribution-options
                                                                         [0 0]
                                                                         "None")]
        [element/text-field (conj db-path :source-name) "Source name"]
        [element/text-field (conj db-path :source-link) "Source link"]
        [element/text-field (conj db-path :source-creator-name) "Creator name"]
        [element/text-field (conj db-path :source-creator-link) "Creator link"]])
     [:div {:style {:margin-bottom "1em"}} " "]]))
