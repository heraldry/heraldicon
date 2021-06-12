(ns heraldry.frontend.form.charge-group
  (:require [heraldry.coat-of-arms.charge-group.core :as charge-group]
            [heraldry.coat-of-arms.charge-group.options :as charge-group-options]
            [heraldry.coat-of-arms.default :as default]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.render :as render]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.frontend.form.charge :as charge]
            [heraldry.frontend.form.element :as element]
            [heraldry.frontend.form.position :as position]
            [heraldry.frontend.form.shared :as shared]
            [heraldry.frontend.state :as state]
            [re-frame.core :as rf]))

(def preview-tinctures
  [:azure :or :vert :gules :purpure :sable])

(defn preview-form [path render-options]
  (let [charge-group @(rf/subscribe [:get path])
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

(defn slot-number-form [path options current-value]
  [element/range-input nil "Number"
   (-> options :min)
   (-> options :max)
   :default 0
   :value current-value
   :on-change #(rf/dispatch [:set-charge-group-slot-number path %])])

(defn strip-form [path type-str]
  (let [strip-data @(rf/subscribe [:get path])
        options charge-group-options/strip-options
        sanitized-strip-data (options/sanitize strip-data options)
        num-slots (-> strip-data :slots count)
        title (str num-slots
                   (when-not (-> sanitized-strip-data :stretch (= 1))
                     ", stretched")
                   (when-not (-> sanitized-strip-data :offset zero?)
                     ", offset"))]
    [element/component path :charge-group title type-str
     (when (-> options :num-slots)
       [slot-number-form (conj path :slots) (:num-slots options) num-slots])

     (when (-> options :stretch)
       [element/range-input (conj path :stretch) "Stretch"
        (-> options :stretch :min)
        (-> options :stretch :max)
        :step 0.01
        :default (options/get-value (:stretch strip-data) (:stretch options))])

     (when (-> options :offset)
       [element/range-input (conj path :offset) "Offset"
        (-> options :offset :min)
        (-> options :offset :max)
        :step 0.01
        :default (options/get-value (:offset strip-data) (:offset options))])]))

(defn charge-group-preset-choice [path group display-name]
  (let [{:keys [result]} (render/coat-of-arms
                          {:escutcheon :rectangle
                           :field {:type :heraldry.field.type/plain
                                   :tincture :argent
                                   :components [group]}}
                          100
                          (-> shared/coa-select-option-context
                              (assoc-in [:render-options :outline?] true)
                              (assoc-in [:render-options :theme] @(rf/subscribe [:get shared/ui-render-options-theme-path]))))]
    [:div.choice.tooltip {:on-click #(state/dispatch-on-event % [:select-charge-group-preset path group])}
     [:svg {:style {:width "4em"
                    :height "4.5em"}
            :viewBox "0 0 120 200"
            :preserveAspectRatio "xMidYMin slice"}
      [:g {:filter "url(#shadow)"}
       [:g {:transform "translate(10,10)"}
        result]]]
     [:div.bottom
      [:h3 {:style {:text-align "center"}} display-name]
      [:i]]]))

(def presets
  [["Ordinaries"
    ["Fesswise" {:type :heraldry.charge-group.type/rows
                 :strip-angle 0
                 :spacing 25
                 :stretch 0.866
                 :charges [{:type :heraldry.charge.type/roundel
                            :field {:type :heraldry.field.type/plain
                                    :tincture :azure}
                            :hints {:outline-mode :keep}}]
                 :strips [{:slots [0 0 0]}]}]
    ["Palewise" {:type :heraldry.charge-group.type/columns
                 :strip-angle 0
                 :spacing 20
                 :stretch 0.866
                 :charges [{:type :heraldry.charge.type/roundel
                            :field {:type :heraldry.field.type/plain
                                    :tincture :azure}
                            :hints {:outline-mode :keep}}]
                 :strips [{:slots [0 0 0]}]}]
    ["Bendwise" {:type :heraldry.charge-group.type/rows
                 :strip-angle 45
                 :spacing 25
                 :stretch 0.866
                 :charges [{:type :heraldry.charge.type/roundel
                            :field {:type :heraldry.field.type/plain
                                    :tincture :azure}
                            :hints {:outline-mode :keep}}]
                 :strips [{:slots [0 0 0]}]}]
    ["Bendwise (sinister)" {:type :heraldry.charge-group.type/rows
                            :strip-angle -45
                            :spacing 25
                            :stretch 0.866
                            :charges [{:type :heraldry.charge.type/roundel
                                       :field {:type :heraldry.field.type/plain
                                               :tincture :azure}
                                       :hints {:outline-mode :keep}}]
                            :strips [{:slots [0 0 0]}]}]
    ["Chevronwise" {:type :heraldry.charge-group.type/rows
                    :strip-angle 0
                    :spacing 30
                    :stretch 0.6
                    :charges [{:type :heraldry.charge.type/roundel
                               :field {:type :heraldry.field.type/plain
                                       :tincture :azure}
                               :hints {:outline-mode :keep}}]
                    :strips [{:slots [0]}
                             {:slots [0 0]}
                             {:slots [0 nil 0]}]}]
    ["In cross" {:type :heraldry.charge-group.type/rows
                 :strip-angle 0
                 :spacing 20
                 :stretch 1
                 :charges [{:type :heraldry.charge.type/roundel
                            :field {:type :heraldry.field.type/plain
                                    :tincture :azure}
                            :hints {:outline-mode :keep}}]
                 :strips [{:slots [nil nil 0 nil nil]}
                          {:slots [nil nil 0 nil nil]}
                          {:slots [0 0 0 0 0]}
                          {:slots [nil nil 0 nil nil]}
                          {:slots [nil nil 0 nil nil]}]}]
    ["In saltire" {:type :heraldry.charge-group.type/rows
                   :strip-angle 45
                   :spacing 20
                   :stretch 1
                   :charges [{:type :heraldry.charge.type/roundel
                              :field {:type :heraldry.field.type/plain
                                      :tincture :azure}
                              :hints {:outline-mode :keep}}]
                   :strips [{:slots [nil nil 0 nil nil]}
                            {:slots [nil nil 0 nil nil]}
                            {:slots [0 0 0 0 0]}
                            {:slots [nil nil 0 nil nil]}
                            {:slots [nil nil 0 nil nil]}]}]
    ["In pall" {:type :heraldry.charge-group.type/columns
                :strip-angle 0
                :spacing 14
                :stretch 0.866
                :charges [{:type :heraldry.charge.type/roundel
                           :field {:type :heraldry.field.type/plain
                                   :tincture :azure}
                           :geometry {:size 18}
                           :hints {:outline-mode :keep}}]
                :strips [{:slots [0 nil nil nil nil]}
                         {:slots [nil 0 nil nil nil]}
                         {:slots [nil nil 0 0 0]
                          :stretch 1.25}
                         {:slots [nil 0 nil nil nil]}
                         {:slots [0 nil nil nil nil]}]}]
    ["Three in above bend" {:type :heraldry.charge-group.type/rows
                            :strip-angle 0
                            :spacing 30
                            :stretch 1
                            :charges [{:type :heraldry.charge.type/roundel
                                       :field {:type :heraldry.field.type/plain
                                               :tincture :azure}
                                       :hints {:outline-mode :keep}}]
                            :strips [{:slots [0 0]}
                                     {:slots [nil 0]}]}]
    ["Three below bend" {:type :heraldry.charge-group.type/rows
                         :strip-angle 45
                         :spacing 50
                         :stretch 0.3
                         :charges [{:type :heraldry.charge.type/roundel
                                    :field {:type :heraldry.field.type/plain
                                            :tincture :azure}
                                    :hints {:outline-mode :keep}}]
                         :strips [{:slots [0 0]}
                                  {:slots [0]}]}]]
   ["Triangular"
    ["Three" {:type :heraldry.charge-group.type/rows
              :strip-angle 0
              :spacing 40
              :stretch 0.866
              :charges [{:type :heraldry.charge.type/roundel
                         :field {:type :heraldry.field.type/plain
                                 :tincture :azure}
                         :hints {:outline-mode :keep}}]
              :strips [{:slots [0 0]}
                       {:slots [0]}]}]
    ["Three (inverted)" {:type :heraldry.charge-group.type/rows
                         :strip-angle 0
                         :spacing 40
                         :stretch 0.866
                         :charges [{:type :heraldry.charge.type/roundel
                                    :field {:type :heraldry.field.type/plain
                                            :tincture :azure}
                                    :hints {:outline-mode :keep}}]
                         :strips [{:slots [0]}
                                  {:slots [0 0]}]}]

    ["Three (columns)" {:type :heraldry.charge-group.type/columns
                        :strip-angle 0
                        :spacing 30
                        :stretch 0.866
                        :charges [{:type :heraldry.charge.type/roundel
                                   :field {:type :heraldry.field.type/plain
                                           :tincture :azure}
                                   :hints {:outline-mode :keep}}]
                        :strips [{:slots [0 0]}
                                 {:slots [0]}]}]
    ["Three (columns, inverted)" {:type :heraldry.charge-group.type/columns
                                  :strip-angle 0
                                  :spacing 30
                                  :stretch 0.866
                                  :charges [{:type :heraldry.charge.type/roundel
                                             :field {:type :heraldry.field.type/plain
                                                     :tincture :azure}
                                             :hints {:outline-mode :keep}}]
                                  :strips [{:slots [0]}
                                           {:slots [0 0]}]}]
    ["Six" {:type :heraldry.charge-group.type/rows
            :strip-angle 0
            :spacing 30
            :stretch 0.866
            :charges [{:type :heraldry.charge.type/roundel
                       :field {:type :heraldry.field.type/plain
                               :tincture :azure}
                       :hints {:outline-mode :keep}}]
            :strips [{:slots [0 0 0]}
                     {:slots [0 0]}
                     {:slots [0]}]}]]
   ["Grid"
    ["Square" {:type :heraldry.charge-group.type/rows
               :strip-angle 0
               :spacing 25
               :stretch 1
               :charges [{:type :heraldry.charge.type/roundel
                          :field {:type :heraldry.field.type/plain
                                  :tincture :azure}
                          :hints {:outline-mode :keep}}]
               :strips [{:slots [0 0 0]}
                        {:slots [0 0 0]}
                        {:slots [0 0 0]}]}]
    ["Diamond" {:type :heraldry.charge-group.type/rows
                :strip-angle 45
                :spacing 25
                :stretch 1
                :charges [{:type :heraldry.charge.type/roundel
                           :field {:type :heraldry.field.type/plain
                                   :tincture :azure}
                           :hints {:outline-mode :keep}}]
                :strips [{:slots [0 0 0]}
                         {:slots [0 0 0]}
                         {:slots [0 0 0]}]}]
    ["Semy rows" {:type :heraldry.charge-group.type/rows
                  :strip-angle 0
                  :spacing 20
                  :stretch 0.866
                  :charges [{:type :heraldry.charge.type/roundel
                             :field {:type :heraldry.field.type/plain
                                     :tincture :azure}
                             :hints {:outline-mode :keep}}]
                  :strips [{:slots [0 0 0 0]}
                           {:slots [0 0 0]}
                           {:slots [0 0 0 0]}
                           {:slots [0 0 0]}
                           {:slots [0 0 0 0]}]}]
    ["Semy columns" {:type :heraldry.charge-group.type/columns
                     :strip-angle 0
                     :spacing 17
                     :stretch 0.866
                     :charges [{:type :heraldry.charge.type/roundel
                                :field {:type :heraldry.field.type/plain
                                        :tincture :azure}
                                :hints {:outline-mode :keep}}]
                     :strips [{:slots [0 0 0 0]}
                              {:slots [0 0 0]}
                              {:slots [0 0 0 0]}
                              {:slots [0 0 0]}
                              {:slots [0 0 0 0]}]}]
    ["Frame" {:type :heraldry.charge-group.type/rows
              :strip-angle 0
              :spacing 17
              :stretch 1
              :charges [{:type :heraldry.charge.type/roundel
                         :field {:type :heraldry.field.type/plain
                                 :tincture :azure}
                         :hints {:outline-mode :keep}}]
              :strips [{:slots [0 0 0 0 0]}
                       {:slots [0 nil nil nil 0]}
                       {:slots [0 nil nil nil 0]}
                       {:slots [0 nil nil nil 0]}
                       {:slots [0 0 0 0 0]}]}]]
   ["Arc"
    ["In annullo" {:type :heraldry.charge-group.type/arc
                   :charges [{:type :heraldry.charge.type/roundel
                              :field {:type :heraldry.field.type/plain
                                      :tincture :azure}
                              :hints {:outline-mode :keep}}]
                   :slots [0 0 0 0 0 0 0]}]
    ["Arc" {:type :heraldry.charge-group.type/arc
            :start-angle 10
            :arc-angle 180
            :charges [{:type :heraldry.charge.type/roundel
                       :field {:type :heraldry.field.type/plain
                               :tincture :azure}
                       :hints {:outline-mode :keep}}]
            :slots [0 0 0 0 0]}]
    ["In annullo (point to center)" {:type :heraldry.charge-group.type/arc
                                     :rotate-charges? true
                                     :charges [{:type :heraldry.charge.type/billet
                                                :field {:type :heraldry.field.type/plain
                                                        :tincture :azure}
                                                :hints {:outline-mode :keep}}]
                                     :slots [0 0 0 0 0 0 0]}]
    ["In annullo (follow)" {:type :heraldry.charge-group.type/arc
                            :rotate-charges? true
                            :charges [{:type :heraldry.charge.type/billet
                                       :anchor {:point :angle
                                                :angle 90}
                                       :field {:type :heraldry.field.type/plain
                                               :tincture :azure}
                                       :hints {:outline-mode :keep}}]
                            :slots [0 0 0 0 0 0 0]}]
    ["Sheaf of" {:type :heraldry.charge-group.type/arc
                 :start-angle -45
                 :arc-angle 90
                 :radius 0
                 :rotate-charges? true
                 :charges [{:type :heraldry.charge.type/billet
                            :field {:type :heraldry.field.type/plain
                                    :tincture :azure}
                            :geometry {:size 50
                                       :stretch 3}
                            :hints {:outline-mode :keep}}]
                 :slots [0 0 0]}]]])

(defn form [path & {:keys [parent-field form-for-field]}]
  (let [charge-group @(rf/subscribe [:get path])
        options (charge-group-options/options charge-group)
        title (-> charge-group :type charge-group-options/type-map)
        strips-path (conj path :strips)
        charges-path (conj path :charges)
        strip-type? (-> charge-group
                        :type
                        #{:heraldry.charge-group.type/rows
                          :heraldry.charge-group.type/columns})
        type-str (case (:type charge-group)
                   :heraldry.charge-group.type/rows "Row"
                   :heraldry.charge-group.type/columns "Column"
                   nil)
        strips (:strips charge-group)
        charges (:charges charge-group)
        slots (:slots charge-group)
        render-options  {:theme @(rf/subscribe [:get shared/ui-render-options-theme-path])}]
    [element/component path :charge-group title "Charge group"
     [:div.setting
      [:label "Presets"]
      " "
      [element/submenu path "Select Charge Group Preset" "Select" {:min-width "22em"}
       (for [[group-name & group] presets]
         ^{:key group-name}
         [:<>
          [:h4 group-name]
          (for [[display-name charge-group] group]
            ^{:key display-name}
            [charge-group-preset-choice path charge-group display-name])])]]

     [element/select (conj path :type) "Type" charge-group-options/type-choices
      :on-change #(rf/dispatch [:change-charge-group-type path %])]

     (when (-> options :origin)
       [position/form (conj path :origin)
        :title "Origin"
        :options (:origin options)])

     [preview-form
      path
      render-options]

     (when (-> options :spacing)
       [element/range-input (conj path :spacing) "Spacing"
        (-> options :spacing :min)
        (-> options :spacing :max)
        :step 0.01
        :default (options/get-value (:spacing charge-group) (:spacing options))])
     (when (-> options :stretch)
       [element/range-input (conj path :stretch) (str type-str " stretch")
        (-> options :stretch :min)
        (-> options :stretch :max)
        :step 0.01
        :default (options/get-value (:stretch charge-group) (:stretch options))])
     (when (-> options :strip-angle)
       [element/range-input (conj path :strip-angle) (str type-str " angle")
        (-> options :strip-angle :min)
        (-> options :strip-angle :max)
        :step 1
        :default (options/get-value (:strip-angle charge-group) (:strip-angle options))])

     (when (-> options :num-slots)
       [slot-number-form (conj path :slots) (:num-slots options) (count slots)])

     (when (-> options :radius)
       [element/range-input (conj path :radius) "Radius"
        (-> options :radius :min)
        (-> options :radius :max)
        :step 1
        :default (options/get-value (:radius charge-group) (:radius options))])

     (when (-> options :arc-angle)
       [element/range-input (conj path :arc-angle) "Arc angle"
        (-> options :arc-angle :min)
        (-> options :arc-angle :max)
        :step 1
        :default (options/get-value (:arc-angle charge-group) (:arc-angle options))
        :tooltip "The change from 360° to a lower angle might be surprising. 360° is special, because it ends with the first slot. For other angles the last slot marks the end of the arc."])

     (when (-> options :start-angle)
       [element/range-input (conj path :start-angle) "Start angle"
        (-> options :start-angle :min)
        (-> options :start-angle :max)
        :step 1
        :default (options/get-value (:start-angle charge-group) (:start-angle options))])

     (when (-> options :arc-stretch)
       [element/range-input (conj path :arc-stretch) "Stretch"
        (-> options :arc-stretch :min)
        (-> options :arc-stretch :max)
        :step 0.01
        :default (options/get-value (:arc-stretch charge-group) (:arc-stretch options))])

     (when (-> options :rotate-charges?)
       [element/checkbox (conj path :rotate-charges?) "Rotate charges"])

     (when strip-type?
       [:<>
        [:div {:style {:margin-bottom "0.5em"}}
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
                   [:i.far.fa-trash-alt]])]]))]]])

     [:div {:style {:margin-bottom "0.5em"}}
      [:button {:on-click #(state/dispatch-on-event % [:add-element charges-path default/charge])}
       [:i.fas.fa-plus] " Charge"]]

     [:div.components
      [:ul
       (for [[idx _] (map-indexed vector charges)]
         (let [charge-path (conj charges-path idx)]
           ^{:key idx}
           [:li
            [:div.no-select {:style {:padding-right "10px"
                                     :white-space "nowrap"}}
             [:a (if (zero? idx)
                   {:class "disabled"}
                   {:on-click #(state/dispatch-on-event % [:move-charge-group-charge-down charge-path])})
              [:i.fas.fa-chevron-up]]
             " "
             [:a (if (= idx (dec (count charges)))
                   {:class "disabled"}
                   {:on-click #(state/dispatch-on-event % [:move-charge-group-charge-up charge-path])})
              [:i.fas.fa-chevron-down]]]
            [:div
             [charge/form charge-path
              :parent-field parent-field
              :form-for-field form-for-field
              :part-of-charge-group? true]]
            [:div {:style {:padding-left "10px"}}
             (when (-> charges count (> 1))
               [:a {:on-click #(state/dispatch-on-event % [:remove-charge-group-charge charge-path])}
                [:i.far.fa-trash-alt]])]]))]]]))
