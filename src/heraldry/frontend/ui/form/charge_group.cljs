(ns heraldry.frontend.ui.form.charge-group
  (:require [heraldry.coat-of-arms.charge-group.core :as charge-group]
            [heraldry.coat-of-arms.charge.core :as charge]
            [heraldry.coat-of-arms.default :as default]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.frontend.ui.shared :as shared]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.element.charge-group-preset-select :as charge-group-preset-select]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.interface :as interface]
            [re-frame.core :as rf]))

(def preview-tinctures
  [:azure :or :vert :gules :purpure :sable])

(defn preview-form [path]
  (let [render-options {:theme @(rf/subscribe [:get-value shared/ui-render-options-theme-path])}
        charge-group @(rf/subscribe [:get-value path])
        environment {:width 200
                     :height 200}
        {:keys [slot-positions
                slot-spacing]} (charge-group/calculate-points charge-group environment {:db-path path})
        dot-size (/ (min (:width slot-spacing)
                         (:height slot-spacing))
                    2
                    1.05)
        num-charges (-> charge-group :charges count)]
    [:div
     [:svg {:style {:width "10em"
                    :height "10em"}
            :viewBox "0 0 200 200"
            :preserveAspectRatio "xMidYMin meet"}
      [:g
       [:rect {:x 0
               :y 0
               :width 200
               :height 200
               :style {:stroke "#000"
                       :fill "none"}}]
       [:g {:transform "translate(100,100)"}
        (for [[idx {:keys [point charge-index slot-path]}] (map-indexed vector slot-positions)]
          (let [color (if (nil? charge-index)
                        "#fff"
                        (-> charge-index
                            (mod (count preview-tinctures))
                            (->> (get preview-tinctures))
                            (tincture/pick render-options)))]
            ^{:key idx}
            [:g {:transform (str "translate(" (:x point) "," (:y point) ")")
                 :on-click #(state/dispatch-on-event % [:cycle-charge-index slot-path num-charges])
                 :style {:cursor "pointer"}}
             [:circle {:r dot-size
                       :style {:stroke "#000"
                               :stroke-width 0.5
                               :fill color}}]
             (when (>= charge-index (count preview-tinctures))
               [:circle {:r (* 2 (quot charge-index (count preview-tinctures)))
                         :style {:stroke "#000"
                                 :stroke-width 0.5
                                 :fill "#fff"}}])]))]]]
     [:div.tooltip.info {:style {:display "inline-block"
                                 :margin-left "0.2em"
                                 :vertical-align "top"}}
      [:i.fas.fa-question-circle]
      [:div.bottom
       [:h3 {:style {:text-align "center"}} "Click the slots to disable them or cycle through the available charges (added below)."]
       [:i]]]]))

(defn strip-form [path type-str]
  (let [strip-data @(rf/subscribe [:get-value path])
        strip-options @(rf/subscribe [:get-relevant-options path])
        sanitized-strip-data (options/sanitize strip-data strip-options)
        num-slots (-> strip-data :slots count)
        title (str num-slots
                   " slot" (when (not= num-slots 1) "s")
                   (when-not (-> sanitized-strip-data :stretch (= 1))
                     ", stretched")
                   (when-not (-> sanitized-strip-data :offset zero?)
                     ", offset"))]
    [:div {:style {:position "relative"}}
     [submenu/submenu path type-str title {:width "22em"}
      (for [option [:slots
                    :stretch
                    :offset]]
        ^{:key option} [interface/form-element (conj path option)])]]))

(defn form [path _]
  (let [component-data @(rf/subscribe [:get-value path])
        strip-type? (-> component-data
                        :type
                        #{:heraldry.charge-group.type/rows
                          :heraldry.charge-group.type/columns})
        type-str (case (:type component-data)
                   :heraldry.charge-group.type/rows "Row"
                   :heraldry.charge-group.type/columns "Column"
                   nil)
        strips (:strips component-data)
        strips-path (conj path :strips)]
    [:div {:style {:display "table"
                   :width "100%"}}
     [:div {:style {:display "table-row"}}
      [:div {:style {:display "table-cell"
                     :vertical-align "top"}}
       [charge-group-preset-select/charge-group-preset-select path]
       (for [option [:type
                     :origin
                     :spacing
                     :stretch
                     :strip-angle
                     :radius
                     :arc-angle
                     :start-angle
                     :arc-stretch
                     :rotate-charges?
                     :slots]]
         ^{:key option} [interface/form-element (conj path option)])

       (when strip-type?
         [:<>
          [:div {:style {:margin-top "1em"
                         :margin-bottom "0.5em"}}
           [:button {:on-click #(state/dispatch-on-event % [:add-element strips-path default/charge-group-strip])}
            [:i.fas.fa-plus] " " type-str]]

          [:div.components
           [:ul
            (for [[idx _] (map-indexed vector strips)]
              (let [strip-path (conj strips-path idx)]
                ^{:key idx}
                [:li
                 [:div.no-select {:style {:padding-right "10px"
                                          :white-space "nowrap"}}
                  [:a (if (zero? idx)
                        {:class "disabled"}
                        {:on-click #(state/dispatch-on-event % [:move-element-down strip-path])})
                   [:i.fas.fa-chevron-up]]
                  " "
                  [:a (if (= idx (dec (count strips)))
                        {:class "disabled"}
                        {:on-click #(state/dispatch-on-event % [:move-element-up strip-path])})
                   [:i.fas.fa-chevron-down]]]
                 [:div
                  [strip-form strip-path type-str]]
                 [:div {:style {:padding-left "10px"}}
                  (when (-> strips count (> 1))
                    [:a {:on-click #(state/dispatch-on-event % [:remove-element strip-path])}
                     [:i.far.fa-trash-alt]])]]))]]])]
      [:div {:style {:display "table-cell"
                     :vertical-align "top"}}
       [preview-form path]]]]))

(defmethod interface/component-node-data :heraldry.component/charge-group [path component-data _component-options]
  {:title (str "Charge group of " (if (-> component-data :charges count (= 1))
                                    (charge/title (-> component-data :charges first))
                                    "various"))
   :buttons [{:icon "fas fa-plus"
              :title "Add"
              :menu [{:title "Charge"
                      :handler #(state/dispatch-on-event % [:add-element (conj path :charges) default/charge])}]}]
   :nodes (concat (->> component-data
                       :charges
                       count
                       range
                       reverse
                       (map (fn [idx]
                              (let [charge-path (conj path :charges idx)]
                                {:path charge-path
                                 :buttons [{:icon "fas fa-chevron-down"
                                            :disabled? (zero? idx)
                                            :tooltip "move down"
                                            :handler #(state/dispatch-on-event % [:move-charge-group-charge-down charge-path])}
                                           {:icon "fas fa-chevron-up"
                                            :disabled? (-> component-data :charges count dec (= idx))
                                            :tooltip "move up"
                                            :handler #(state/dispatch-on-event % [:move-charge-group-charge-up charge-path])}
                                           {:icon "far fa-trash-alt"
                                            :disabled? (-> component-data :charges count (= 1))
                                            :tooltip "remove"
                                            :handler #(state/dispatch-on-event
                                                       % [:remove-charge-group-charge charge-path])}]})))
                       vec))})

(defmethod interface/component-form-data :heraldry.component/charge-group [_path _component-data _component-options]
  {:form form})
