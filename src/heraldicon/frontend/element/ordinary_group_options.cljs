(ns heraldicon.frontend.element.ordinary-group-options
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.interface :as interface]))

(defn- part-of-group? [context ordinary-type]
  (let [field-context (interface/parent context)
        {:keys [affected-paths]} (interface/get-auto-ordinary-info ordinary-type field-context)]
    (get affected-paths (:path context))))

(defn elements [ordinary-context]
  (let [field-context (interface/parent ordinary-context)
        fess-group-context (c/++ field-context :fess-group)
        pale-group-context (c/++ field-context :pale-group)
        part-of-fess-group? (part-of-group? ordinary-context :heraldry.ordinary.type/fess)
        part-of-pale-group? (part-of-group? ordinary-context :heraldry.ordinary.type/pale)]
    [:<>
     (when part-of-fess-group?
       [:<>
        [:div {:style {:font-size "1.3em"
                       :margin-top "0.5em"
                       :margin-bottom "0.5em"}} [tr :string.option.section/fess-group]]
        (element/elements
         fess-group-context
         [:default-size
          :default-spacing
          :offset-y])])

     (when part-of-pale-group?
       [:<>
        [:div {:style {:font-size "1.3em"
                       :margin-top "0.5em"
                       :margin-bottom "0.5em"}} [tr :string.option.section/pale-group]]
        (element/elements
         pale-group-context
         [:default-size
          :default-spacing
          :offset-x])])]))
