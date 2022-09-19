(ns heraldicon.frontend.element.ordinary-group-options
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.interface :as interface]))

(defn elements [context]
  (let [fess-group-context (c/++ context :fess-group)
        pale-group-context (c/++ context :pale-group)]
    [:<>
     (when (interface/get-options fess-group-context)
       [:<>
        [:div {:style {:font-size "1.3em"
                       :margin-top "0.5em"
                       :margin-bottom "0.5em"}} [tr :string.option.section/fess-group]]
        (element/elements
         fess-group-context
         [:default-size
          :default-bottom-margin
          :offset-y])])

     (when (interface/get-options pale-group-context)
       [:<>
        [:div {:style {:font-size "1.3em"
                       :margin-top "0.5em"
                       :margin-bottom "0.5em"}} [tr :string.option.section/pale-group]]
        (element/elements
         pale-group-context
         [:default-size
          :default-left-margin
          :offset-x])])]))
