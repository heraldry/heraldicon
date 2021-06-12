(ns heraldry.frontend.form.charge-group
  (:require [heraldry.coat-of-arms.charge-group.core :as charge-group]
            [heraldry.coat-of-arms.charge-group.options :as charge-group-options]
            [heraldry.coat-of-arms.default :as default]
            [heraldry.coat-of-arms.options :as options]
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
                    2)
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
                                 :fill "#fff"}}])]))]]]]))

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
       [element/range-input nil "Number"
        (-> options :num-slots :min)
        (-> options :num-slots :max)
        :default (options/get-value (:num-slots strip-data) (:num-slots options))
        :value num-slots
        :on-change #(rf/dispatch [:set-charge-group-slot-number (conj path :slots) %])])

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

(defn form [path & {:keys [parent-field form-for-field]}]
  (let [charge-group @(rf/subscribe [:get path])
        options (charge-group-options/options charge-group)
        title (-> charge-group :type charge-group-options/type-map)
        strips-path (conj path :strips)
        charges-path (conj path :charges)
        type-str (case (:type charge-group)
                   :heraldry.charge-group.type/rows "Row"
                   :heraldry.charge-group.type/columns "Column")
        strips (:strips charge-group)
        charges (:charges charge-group)
        render-options  {:theme @(rf/subscribe [:get shared/ui-render-options-theme-path])}]
    [element/component path :charge-group title "Charge group"
     [element/select (conj path :type) "Type" charge-group-options/type-choices]
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
                [:i.far.fa-trash-alt]])]]))]]

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
