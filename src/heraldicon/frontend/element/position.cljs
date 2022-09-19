(ns heraldicon.frontend.element.position
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.element.submenu :as submenu]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]
   [heraldicon.options :as options]))

;; TODO: probably can be improved with better subscriptions
(defn- submenu-link-name [options position]
  (let [changes [(-> position
                     :point
                     position/orientation-point-map)
                 (when (some #(options/changed? % position options)
                             [:offset-x :offset-y :angle])
                   "adjusted")
                 (when (options/changed? :alignment position options)
                   "aligned")]]
    (string/upper-case-first (string/combine ", " changes))))

(defmethod element/element :ui.element/position [context]
  (when-let [options (interface/get-options context)]
    (let [{:ui/keys [label]} options
          link-name (submenu-link-name options (interface/get-sanitized-data context))
          field-context (-> context c/-- interface/parent)
          fess-group-context (c/++ field-context :fess-group)
          pale-group-context (c/++ field-context :pale-group)]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context label [tr link-name] {:style {:width "24em"}
                                                       :class "submenu-position"}
         (element/elements
          context
          [:point
           :alignment
           :angle
           :bottom-margin
           :left-margin
           :offset-x
           :offset-y
           :type])

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
              :offset-x])])]]])))
