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
        chevron-group-context (c/++ field-context :chevron-group)
        bend-group-context (c/++ field-context :bend-group)
        bend-sinister-group-context (c/++ field-context :bend-sinister-group)
        part-of-fess-group? (part-of-group? ordinary-context :heraldry.ordinary.type/fess)
        part-of-pale-group? (part-of-group? ordinary-context :heraldry.ordinary.type/pale)
        part-of-chevron-group? (part-of-group? ordinary-context :heraldry.ordinary.type/chevron)
        part-of-bend-group? (part-of-group? ordinary-context :heraldry.ordinary.type/bend)
        part-of-bend-sinister-group? (part-of-group? ordinary-context :heraldry.ordinary.type/bend-sinister)]
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
          :offset-x])])

     (when part-of-chevron-group?
       [:<>
        [:div {:style {:font-size "1.3em"
                       :margin-top "0.5em"
                       :margin-bottom "0.5em"}} [tr :string.option.section/chevron-group]]
        (element/elements
         chevron-group-context
         [:origin
          :orientation
          :default-size
          :default-spacing
          :offset-x
          :offset-y])])

     (when part-of-bend-group?
       [:<>
        [:div {:style {:font-size "1.3em"
                       :margin-top "0.5em"
                       :margin-bottom "0.5em"}} [tr :string.option.section/bend-group]]
        (element/elements
         bend-group-context
         [:ignore-ordinary-impact?
          :orientation
          :default-size
          :default-spacing
          :offset-y])])

     (when part-of-bend-sinister-group?
       [:<>
        [:div {:style {:font-size "1.3em"
                       :margin-top "0.5em"
                       :margin-bottom "0.5em"}} [tr :string.option.section/bend-sinister-group]]
        (element/elements
         bend-sinister-group-context
         [:ignore-ordinary-impact?
          :orientation
          :default-size
          :default-spacing
          :offset-y])])]))
