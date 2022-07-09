(ns heraldicon.frontend.element.tincture-select
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.element.submenu :as submenu]
   [heraldicon.frontend.element.value-mode-select :as value-mode-select]
   [heraldicon.frontend.js-event :as js-event]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.tooltip :as tooltip]
   [heraldicon.heraldry.default :as default]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]
   [heraldicon.options :as options]
   [re-frame.core :as rf]))

(defn- tincture-choice [context key display-name & {:keys [selected?
                                                           clickable?]
                                                    :or {clickable? true}}]
  (let [width 40
        height 50
        margin 5
        mask-id "m"
        tincture-context (-> context
                             (assoc :data (assoc default/field :tincture key))
                             (c/<< :path [:context :data]))
        choice [:div {:style {:border (if selected?
                                        "1px solid #000"
                                        "1px solid transparent")
                              :border-radius "5px"}}
                [:svg.clickable {:viewBox (str "0 0 " (+ width (* 2 margin)) " " (+ height (* 2 margin)))
                                 :style {:width "4em"}
                                 :on-click (when clickable?
                                             (js-event/handled #(rf/dispatch [:set context key])))}
                 [:defs
                  [:mask {:id mask-id}
                   [:rect {:width width
                           :height height
                           :fill "#ffffff"}]]]
                 [:g {:transform (str "translate(" margin "," margin ")")}
                  [tincture/tinctured-field tincture-context
                   :mask-id mask-id
                   :transform (str "translate(" (- margin) "," (- margin) ")")]
                  [:rect {:width width
                          :height height
                          :stroke "#0f0f0f"
                          :stroke-width "0.5px"
                          :stroke-linejoin "round"
                          :fill "none"}]]]]]
    (if clickable?
      [tooltip/choice display-name choice]
      choice)))

(defn tincture-select [context & {:keys [default-option]}]
  (when-let [option (or (interface/get-relevant-options context)
                        default-option)]
    (let [current-value (interface/get-raw-data context)
          {:keys [choices]
           :ui/keys [label]} option
          value (options/get-value current-value option)
          choice-map (options/choices->map choices)
          choice-name (get choice-map value)
          label (or label :string.option/tincture)]
      [:div.ui-setting
       (when label
         [:label [tr label]])
       [:div.option
        [submenu/submenu context :string.option/select-tincture
         [:div
          [:div
           [tr choice-name]
           [value-mode-select/value-mode-select context :default-option default-option]]
          [:div {:style {:transform "translate(-0.45em,0)"}}
           [tincture-choice context value choice-name :clickable? false]]]
         {:style {:width "22em"}}
         (into [:<>]
               (map (fn [[group-name & group]]
                      (into
                       ^{:key group-name}
                       [:<>
                        [:h4 [tr group-name]]]
                       (map (fn [[display-name key]]
                              ^{:key display-name}
                              [tincture-choice context key display-name :selected? (= key value)]))
                       group)))
               choices)]]])))

(defmethod element/element :ui.element/tincture-select [context]
  [tincture-select context])
