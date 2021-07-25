(ns heraldry.frontend.ui.form.charge-group
  (:require [heraldry.coat-of-arms.charge-group.core :as charge-group]
            [heraldry.coat-of-arms.charge.core :as charge]
            [heraldry.coat-of-arms.default :as default]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.element.charge-group-preset-select :as charge-group-preset-select]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.interface :as ui-interface]
            [heraldry.interface :as interface]
            [re-frame.core :as rf]))

(rf/reg-event-db :cycle-charge-index
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

(rf/reg-event-db :remove-charge-group-charge
  (fn [db [_ path]]
    (let [elements-path (drop-last path)
          strips-path (-> path
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
          (update-in strips-path (fn [strips]
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

(rf/reg-event-db :move-charge-group-charge-up
  (fn [db [_ path]]
    (let [elements-path (drop-last path)
          strips-path (-> path
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
          (update-in strips-path (fn [strips]
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

(rf/reg-event-fx :add-charge-group-strip
  (fn [{:keys [db]} [_ path value]]
    (let [elements (-> (get-in db path)
                       (conj value)
                       vec)]
      {:db (assoc-in db path elements)})))

(rf/reg-event-db :move-charge-group-charge-down
  (fn [db [_ path]]
    (let [elements-path (drop-last path)
          strips-path (-> path
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
          (update-in strips-path (fn [strips]
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
  (let [context {:render-options-path [:example-coa :render-options]}
        environment {:width 200
                     :height 200}
        {:keys [slot-positions
                slot-spacing]} (charge-group/calculate-points path environment context)
        num-charges (interface/get-list-size (conj path :charges) context)
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
                                  :fill "#fff"}}])])))]]]
     [:div.tooltip.info {:style {:display "inline-block"
                                 :margin-left "0.2em"
                                 :vertical-align "top"}}
      [:i.fas.fa-question-circle]
      [:div.bottom
       [:h3 {:style {:text-align "center"}} "Click the slots to disable them or cycle through the available charges (added below)."]
       [:i]]]]))

(defn strip-form [path type-str]
  (let [num-slots @(rf/subscribe [:get-list-size (conj path :slots)])
        stretch @(rf/subscribe [:get-sanitized-value (conj path :stretch)])
        offset @(rf/subscribe [:get-sanitized-value (conj path :offset)])
        title (str num-slots
                   " slot" (when (not= num-slots 1) "s")
                   (when-not (= stretch 1)
                     ", stretched")
                   (when-not (zero? offset)
                     ", offset"))]
    [:div {:style {:position "relative"}}
     [submenu/submenu path type-str title {:width "22em"}
      (for [option [:slots
                    :stretch
                    :offset]]
        ^{:key option} [ui-interface/form-element (conj path option)])]]))

(defn form [path _]
  (let [charge-group-type @(rf/subscribe [:get-value (conj path :type)])
        strip-type? (#{:heraldry.charge-group.type/rows
                       :heraldry.charge-group.type/columns}
                     charge-group-type)
        type-str (case charge-group-type
                   :heraldry.charge-group.type/rows "Row"
                   :heraldry.charge-group.type/columns "Column"
                   nil)
        strips-path (conj path :strips)
        num-strips @(rf/subscribe [:get-list-size strips-path])]
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
         ^{:key option} [ui-interface/form-element (conj path option)])

       (when strip-type?
         [:div.ui-setting
          [:label (str type-str "s ")
           [:button {:on-click #(state/dispatch-on-event % [:add-charge-group-strip strips-path default/charge-group-strip])}
            [:i.fas.fa-plus] " Add"]]

          [:div.option.charge-group-strips
           [:ul
            (doall
             (for [idx (range num-strips)]
               (let [strip-path (conj strips-path idx)]
                 ^{:key idx}
                 [:li
                  [:div.no-select {:style {:padding-right "10px"
                                           :white-space "nowrap"}}
                   [:a (if (zero? idx)
                         {:class "disabled"}
                         {:on-click #(state/dispatch-on-event % [:move-element strip-path (dec idx)])})
                    [:i.fas.fa-chevron-up]]
                   " "
                   [:a (if (= idx (dec num-strips))
                         {:class "disabled"}
                         {:on-click #(state/dispatch-on-event % [:move-element strip-path (inc idx)])})
                    [:i.fas.fa-chevron-down]]]
                  [:div
                   [strip-form strip-path type-str]]
                  [:div {:style {:padding-left "10px"}}
                   [:a (if (< num-strips 2)
                         {:class "disabled"}
                         {:on-click #(state/dispatch-on-event % [:remove-element strip-path])})
                    [:i.far.fa-trash-alt]]]])))]]])

       [ui-interface/form-element (conj path :manual-blazon)]]
      [:div {:style {:display "table-cell"
                     :vertical-align "top"}}
       [preview-form path]]]]))

(defmethod ui-interface/component-node-data :heraldry.component/charge-group [path]
  (let [num-charges @(rf/subscribe [:get-list-size (conj path :charges)])]
    {:title (str "Charge group of " (if (= num-charges 1)
                                      (charge/title (conj path :charges 0) {})
                                      "various"))
     :buttons [{:icon "fas fa-plus"
                :title "Add"
                :menu [{:title "Charge"
                        :handler #(state/dispatch-on-event % [:add-element (conj path :charges) default/charge])}]}]
     :nodes (concat (->> (range num-charges)
                         reverse
                         (map (fn [idx]
                                (let [charge-path (conj path :charges idx)]
                                  {:path charge-path
                                   :buttons [{:icon "fas fa-chevron-down"
                                              :disabled? (zero? idx)
                                              :tooltip "move down"
                                              :handler #(state/dispatch-on-event % [:move-charge-group-charge-down charge-path])}
                                             {:icon "fas fa-chevron-up"
                                              :disabled? (= idx (dec num-charges))
                                              :tooltip "move up"
                                              :handler #(state/dispatch-on-event % [:move-charge-group-charge-up charge-path])}
                                             {:icon "far fa-trash-alt"
                                              :disabled? (= num-charges 1)
                                              :tooltip "remove"
                                              :handler #(state/dispatch-on-event % [:remove-charge-group-charge charge-path])}]})))
                         vec))}))

(defmethod ui-interface/component-form-data :heraldry.component/charge-group [_path]
  {:form form})
