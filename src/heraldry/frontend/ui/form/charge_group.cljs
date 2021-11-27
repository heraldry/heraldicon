(ns heraldry.frontend.ui.form.charge-group
  (:require
   [heraldry.coat-of-arms.charge-group.core :as charge-group]
   [heraldry.coat-of-arms.charge.options :as charge-options]
   [heraldry.coat-of-arms.default :as default]
   [heraldry.coat-of-arms.tincture.core :as tincture]
   [heraldry.context :as c]
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.macros :as macros]
   [heraldry.frontend.state :as state]
   [heraldry.frontend.ui.element.charge-group-preset-select
    :as charge-group-preset-select]
   [heraldry.frontend.ui.element.submenu :as submenu]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.interface :as interface]
   [heraldry.math.vector :as v]
   [heraldry.static :as static]
   [heraldry.strings :as strings]
   [heraldry.util :as util]))

(macros/reg-event-db :cycle-charge-index
  (fn [db [_ path num-charges]]
    (let [slots-path (drop-last path)
          slot-index (last path)
          slots (get-in db slots-path)
          current-value (get-in db path)
          new-value (cond
                      (nil? current-value) 0
                      (= current-value (dec num-charges)) nil
                      (> current-value (dec num-charges)) 0
                      :else (inc current-value))]
      (assoc-in db slots-path (assoc slots slot-index new-value)))))

(macros/reg-event-db :remove-charge-group-charge
  (fn [db [_ path]]
    (let [elements-path (drop-last path)
          strips-context (-> path
                             (->> (drop-last 2))
                             vec
                             (conj :strips))
          slots-path (-> path
                         (->> (drop-last 2))
                         vec
                         (conj :slots))
          index (last path)]
      (-> db
          (update-in elements-path (fn [elements]
                                     (vec (concat (subvec elements 0 index)
                                                  (subvec elements (inc index))))))
          (update-in strips-context (fn [strips]
                                      (mapv (fn [strip]
                                              (update strip :slots (fn [slots]
                                                                     (mapv (fn [charge-index]
                                                                             (cond
                                                                               (= charge-index index) 0
                                                                               (> charge-index index) (dec charge-index)
                                                                               :else charge-index))
                                                                           slots))))
                                            strips)))
          (update-in slots-path (fn [slots]
                                  (mapv (fn [charge-index]
                                          (cond
                                            (= charge-index index) 0
                                            (> charge-index index) (dec charge-index)
                                            :else charge-index))
                                        slots)))
          (state/element-order-changed elements-path index nil)))))

(macros/reg-event-db :move-charge-group-charge-up
  (fn [db [_ path]]
    (let [elements-path (drop-last path)
          strips-context (-> path
                             (->> (drop-last 2))
                             vec
                             (conj :strips))
          slots-path (-> path
                         (->> (drop-last 2))
                         vec
                         (conj :slots))
          index (last path)]
      (-> db
          (update-in elements-path (fn [elements]
                                     (let [num-elements (count elements)]
                                       (if (>= index num-elements)
                                         elements
                                         (-> elements
                                             (subvec 0 index)
                                             (conj (get elements (inc index)))
                                             (conj (get elements index))
                                             (concat (subvec elements (+ index 2)))
                                             vec)))))
          (update-in strips-context (fn [strips]
                                      (mapv (fn [strip]
                                              (update strip :slots (fn [slots]
                                                                     (mapv (fn [charge-index]
                                                                             (cond
                                                                               (= charge-index index) (inc charge-index)
                                                                               (= charge-index (inc index)) (dec charge-index)
                                                                               :else charge-index))
                                                                           slots))))
                                            strips)))
          (update-in slots-path (fn [slots]
                                  (mapv (fn [charge-index]
                                          (cond
                                            (= charge-index index) (inc charge-index)
                                            (= charge-index (inc index)) (dec charge-index)
                                            :else charge-index))
                                        slots)))
          (state/element-order-changed elements-path index (inc index))))))

(macros/reg-event-db :add-charge-group-strip
  (fn [db [_ {:keys [path]} value]]
    (let [elements (-> (get-in db path)
                       (conj value)
                       vec)]
      (assoc-in db path elements))))

(macros/reg-event-db :move-charge-group-charge-down
  (fn [db [_ path]]
    (let [elements-path (drop-last path)
          strips-context (-> path
                             (->> (drop-last 2))
                             vec
                             (conj :strips))
          slots-path (-> path
                         (->> (drop-last 2))
                         vec
                         (conj :slots))
          index (last path)]
      (-> db
          (update-in elements-path (fn [elements]
                                     (if (zero? index)
                                       elements
                                       (-> elements
                                           (subvec 0 (dec index))
                                           (conj (get elements index))
                                           (conj (get elements (dec index)))
                                           (concat (subvec elements (inc index)))
                                           vec))))
          (update-in strips-context (fn [strips]
                                      (mapv (fn [strip]
                                              (update strip :slots (fn [slots]
                                                                     (mapv (fn [charge-index]
                                                                             (cond
                                                                               (= charge-index (dec index)) (inc charge-index)
                                                                               (= charge-index index) (dec charge-index)
                                                                               :else charge-index))
                                                                           slots))))
                                            strips)))
          (update-in slots-path (fn [slots]
                                  (mapv (fn [charge-index]
                                          (cond
                                            (= charge-index (dec index)) (inc charge-index)
                                            (= charge-index index) (dec charge-index)
                                            :else charge-index))
                                        slots)))
          (state/element-order-changed elements-path index (dec index))))))

(def preview-tinctures
  [:azure :or :vert :gules :purpure :sable])

(defn preview-form [path]
  (let [context {:path path
                 :render-options-path [:example-coa :render-options]
                 :environment {:width 200
                               :height 200
                               :points {:fess {:x 0 :y 0}
                                        :top {:x 0 :y -100}}
                               :shape {:paths ["M-100,-100 h200 v200 h-200 z"]}}}
        {:keys [slot-positions
                slot-spacing]} (charge-group/calculate-points context)
        num-charges (interface/get-list-size (c/++ context :charges))
        dot-size (/ (min (:width slot-spacing)
                         (:height slot-spacing))
                    2
                    1.05)]
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
        (doall
         (for [[idx {:keys [point charge-index slot-path]}] (map-indexed vector slot-positions)]
           (let [color (if (nil? charge-index)
                         "#fff"
                         (-> charge-index
                             (mod (count preview-tinctures))
                             (->> (get preview-tinctures))
                             (tincture/pick context)))]
             ^{:key idx}
             [:g {:transform (str "translate(" (v/->str point) ")")
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
                                  :fill "#fff"}}])])))]]]
     [:div.tooltip.info {:style {:display "inline-block"
                                 :margin-left "0.2em"
                                 :vertical-align "top"}}
      [:i.fas.fa-question-circle]
      [:div.bottom
       [:h3 {:style {:text-align "center"}} "Click the slots to disable them or cycle through the available charges (added below)."]
       [:i]]]]))

(defn strip-form [context type-str]
  (let [num-slots (interface/get-list-size (c/++ context :slots))
        stretch (interface/get-sanitized-data (c/++ context :stretch))
        offset (interface/get-sanitized-data (c/++ context :offset))
        title (util/combine
               ", "
               [(util/str-tr num-slots
                             " " (if (= num-slots 1)
                                   {:en "slot"
                                    :de "Slot"}
                                   {:en "slots"
                                    :de "Slots"}))
                (when-not (= stretch 1)
                  strings/stretched)
                (when-not (zero? offset)
                  strings/shifted)])]
    [:div {:style {:position "relative"}}
     [submenu/submenu context type-str title {:style {:width "20em"}
                                              :class "submenu-strip-form"}
      (ui-interface/form-elements
       context
       [:slots
        :stretch
        :offset])]]))

(defn form [{:keys [path] :as context}]
  (let [charge-group-type (interface/get-raw-data (c/++ context :type))
        strip-type? (#{:heraldry.charge-group.type/rows
                       :heraldry.charge-group.type/columns}
                     charge-group-type)
        type-str (case charge-group-type
                   :heraldry.charge-group.type/rows {:en "Row"
                                                     :de "Zeile"}
                   :heraldry.charge-group.type/columns {:en "Column"
                                                        :de "Spalte"}
                   nil)
        type-plural-str (case charge-group-type
                          :heraldry.charge-group.type/rows {:en "Rows"
                                                            :de "Zeilen"}
                          :heraldry.charge-group.type/columns {:en "Columns"
                                                               :de "Spalten"}
                          nil)]
    [:div {:style {:display "table-cell"
                   :vertical-align "top"}}
     [charge-group-preset-select/charge-group-preset-select context]

     [ui-interface/form-element (c/++ context :origin)]

     [preview-form path]

     (ui-interface/form-elements
      context
      [:type
       :spacing
       :stretch
       :strip-angle
       :radius
       :arc-angle
       :start-angle
       :arc-stretch
       :distance
       :offset
       :rotate-charges?
       :slots])

     (when strip-type?
       (let [strips-context (c/++ context :strips)
             num-strips (interface/get-list-size strips-context)]
         [:div.ui-setting
          [:label [tr type-plural-str]
           [:button {:on-click #(state/dispatch-on-event % [:add-charge-group-strip
                                                            strips-context default/charge-group-strip])}
            [:i.fas.fa-plus] " " [tr strings/add]]]

          [:div.option.charge-group-strips
           [:ul
            (doall
             (for [idx (range num-strips)]
               (let [strip-context (c/++ strips-context idx)]
                 ^{:key idx}
                 [:li
                  [:div.no-select {:style {:padding-right "10px"
                                           :white-space "nowrap"}}
                   [:a (if (zero? idx)
                         {:class "disabled"}
                         {:on-click #(state/dispatch-on-event % [:move-element strip-context (dec idx)])})
                    [:i.fas.fa-chevron-up]]
                   " "
                   [:a (if (= idx (dec num-strips))
                         {:class "disabled"}
                         {:on-click #(state/dispatch-on-event % [:move-element strip-context (inc idx)])})
                    [:i.fas.fa-chevron-down]]]
                  [:div
                   [strip-form strip-context type-str]]
                  [:div {:style {:padding-left "10px"}}
                   [:a (if (< num-strips 2)
                         {:class "disabled"}
                         {:on-click #(state/dispatch-on-event % [:remove-element strip-context])})
                    [:i.far.fa-trash-alt]]]])))]]]))

     [ui-interface/form-element (c/++ context :manual-blazon)]]))

(defmethod ui-interface/component-node-data :heraldry.component/charge-group [context]
  (let [charges-context (c/++ context :charges)
        num-charges (interface/get-list-size charges-context)]
    {:title (util/str-tr {:en "Charge group of "
                          :de "Gruppe von "} (if (= num-charges 1)
                                               (charge-options/title (c/++ context :charges 0))
                                               {:en "various"
                                                :de "verschiedenen"}))
     :icon {:default (static/static-url
                      (str "/svg/charge-group-preset-three.svg"))
            :selected (static/static-url
                       (str "/svg/charge-group-preset-three-selected.svg"))}
     :buttons [{:icon "fas fa-plus"
                :title strings/add
                :menu [{:title strings/charge
                        :handler #(state/dispatch-on-event % [:add-element charges-context default/charge])}]}]
     :nodes (concat (->> (range num-charges)
                         reverse
                         (map (fn [idx]
                                (let [charge-context (c/++ charges-context idx)]
                                  {:context charge-context
                                   :buttons [{:icon "fas fa-chevron-down"
                                              :disabled? (zero? idx)
                                              :tooltip strings/move-down
                                              :handler #(state/dispatch-on-event % [:move-charge-group-charge-down charge-context])}
                                             {:icon "fas fa-chevron-up"
                                              :disabled? (= idx (dec num-charges))
                                              :tooltip strings/move-up
                                              :handler #(state/dispatch-on-event % [:move-charge-group-charge-up charge-context])}
                                             {:icon "far fa-trash-alt"
                                              :disabled? (= num-charges 1)
                                              :tooltip strings/remove
                                              :handler #(state/dispatch-on-event % [:remove-charge-group-charge charge-context])}]})))
                         vec))}))

(defmethod ui-interface/component-form-data :heraldry.component/charge-group [_context]
  {:form form})
