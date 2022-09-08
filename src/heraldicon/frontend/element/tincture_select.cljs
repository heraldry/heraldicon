(ns heraldicon.frontend.element.tincture-select
  (:require
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.element.submenu :as submenu]
   [heraldicon.frontend.element.value-mode-select :as value-mode-select]
   [heraldicon.frontend.js-event :as js-event]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.tooltip :as tooltip]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]
   [heraldicon.options :as options]
   [re-frame.core :as rf]))

(defn preview [color {:keys [scale-x
                             scale-y
                             translate-x
                             translate-y]}]
  (let [mask-id "preview-mask"]
    [:svg {:version "1.1"
           :xmlns "http://www.w3.org/2000/svg"
           :xmlnsXlink "http://www.w3.org/1999/xlink"
           :viewBox (str "0 0 120 140")
           :preserveAspectRatio "xMidYMin slice"
           :style {:width "4em"}}
     [:mask {:id mask-id}
      [:rect {:x 0
              :y 0
              :width 100
              :height 120
              :stroke "none"
              :fill "#fff"}]]
     [:g {:transform "translate(10,10)"}
      [:g {:mask (str "url(#" mask-id ")")}
       [:rect {:x 0
               :y 0
               :width 100
               :height 120
               :stroke "none"
               :fill color
               :transform (str "translate(" translate-x "," translate-y ")"
                               "scale(" scale-x "," scale-y ")")}]]
      [:rect {:x 0
              :y 0
              :width 100
              :height 120
              :stroke "#000"
              :fill "none"}]]]))

(defn- tincture-choice [context key display-name & {:keys [selected?
                                                           clickable?]
                                                    :or {clickable? true}}]
  (let [choice [:div {:class (when clickable?
                               "clickable")
                      :style {:border (if selected?
                                        "1px solid #000"
                                        "1px solid transparent")
                              :border-radius "5px"}
                      :on-click (when clickable?
                                  (js-event/handled #(rf/dispatch [:set context key])))}
                [preview (tincture/pick key context) (if (= key :none)
                                                       {:scale-x 2.5
                                                        :scale-y 2.4
                                                        :translate-x 0
                                                        :translate-y 0}
                                                       {:scale-x 3
                                                        :scale-y 3
                                                        :translate-x -25
                                                        :translate-y 0})]]]
    (if clickable?
      [tooltip/choice display-name choice]
      choice)))

(defn tincture-select [context & {:keys [default-option]}]
  (when-let [option (or (interface/get-options context)
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
