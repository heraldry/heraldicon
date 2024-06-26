(ns heraldicon.frontend.component.charge-group
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.component.core :as component]
   [heraldicon.frontend.component.drag :as drag]
   [heraldicon.frontend.component.element :as-alias component.element]
   [heraldicon.frontend.element.charge-group-preset-select :as charge-group-preset-select]
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.element.submenu :as submenu]
   [heraldicon.frontend.js-event :as js-event]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.tooltip :as tooltip]
   [heraldicon.heraldry.charge-group.core :as charge-group]
   [heraldicon.heraldry.charge.options :as charge.options]
   [heraldicon.heraldry.default :as default]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]
   [heraldicon.math.vector :as v]
   [heraldicon.static :as static]
   [re-frame.core :as rf]))

(macros/reg-event-db ::cycle-charge-index
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

(def ^:private remove-charge-options
  {:post-fn (fn [db path]
              (let [strips-context (-> path
                                       (->> (drop-last 2))
                                       vec
                                       (conj :strips))
                    slots-path (-> path
                                   (->> (drop-last 2))
                                   vec
                                   (conj :slots))
                    index (last path)]
                (-> db
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
                                                  slots))))))})

(macros/reg-event-db ::add-strip
  (fn [db [_ {:keys [path]} value]]
    (let [elements (-> (get-in db path)
                       (conj value)
                       vec)]
      (assoc-in db path elements))))

(def ^:private preview-tinctures
  [:azure :or :vert :gules :purpure :sable])

(defn- preview-form [context]
  (let [{:keys [slot-positions
                slot-spacing]} (charge-group/calculate-points
                                (-> context
                                    (c/set-key :parent-environment-override {:width 200
                                                                             :height 200})
                                    (c/set-key :parent-shape "M-100,-100 h200 v200 h-200 z")))
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
      [:rect {:x 0
              :y 0
              :width 200
              :height 200
              :style {:stroke "#000"
                      :fill "none"}}]
      (into [:g {:transform "translate(100,100)"}]
            (map-indexed (fn [idx {:keys [point charge-index slot-path]}]
                           (let [color (if (nil? charge-index)
                                         "#fff"
                                         (-> charge-index
                                             (mod (count preview-tinctures))
                                             (->> (get preview-tinctures))
                                             (tincture/pick context)))]
                             ^{:key idx}
                             [:g {:transform (str "translate(" (v/->str point) ")")
                                  :on-click (js-event/handled
                                             #(rf/dispatch [::cycle-charge-index slot-path num-charges]))
                                  :style {:cursor "pointer"}}
                              [:circle {:r dot-size
                                        :style {:stroke "#000"
                                                :stroke-width 0.5
                                                :fill color}}]
                              (when (>= charge-index (count preview-tinctures))
                                [:circle {:r (* 2 (quot charge-index (count preview-tinctures)))
                                          :style {:stroke "#000"
                                                  :stroke-width 0.5
                                                  :fill "#fff"}}])])))
            slot-positions)]
     [tooltip/info :string.tooltip/charge-group-preview]]))

(defn- strip-form [context type-str]
  (let [num-slots (interface/get-list-size (c/++ context :slots))
        stretch (interface/get-sanitized-data (c/++ context :stretch))
        offset (interface/get-sanitized-data (c/++ context :offset))
        title (string/combine
               ", "
               [(string/str-tr num-slots
                               " " (if (= num-slots 1)
                                     :string.submenu-summary/slot
                                     :string.submenu-summary/slots))
                (when-not (= stretch 1)
                  :string.submenu-summary/stretched)
                (when-not (zero? offset)
                  :string.submenu-summary/shifted)])]
    [:div {:style {:position "relative"}}
     [submenu/submenu context type-str [tr title] {:style {:width "20em"}
                                                   :class "submenu-strip-form"}
      (element/elements
       context
       [:slots
        :stretch
        :offset])]]))

(defn- form [context]
  (let [charge-group-type (interface/get-raw-data (c/++ context :type))
        strip-type? (#{:heraldry.charge-group.type/rows
                       :heraldry.charge-group.type/columns}
                     charge-group-type)
        type-str (case charge-group-type
                   :heraldry.charge-group.type/rows :string.option/row
                   :heraldry.charge-group.type/columns :string.option/column
                   nil)
        type-plural-str (case charge-group-type
                          :heraldry.charge-group.type/rows :string.charge-group.type/rows
                          :heraldry.charge-group.type/columns :string.charge-group.type/columns
                          nil)]
    [:div {:style {:display "table-cell"
                   :vertical-align "top"}}
     [element/element (c/++ context :adapt-to-ordinaries?)]

     [charge-group-preset-select/charge-group-preset-select context]

     [element/element (c/++ context :anchor)]

     [preview-form context]

     (element/elements
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
           " "
           [:button {:on-click (js-event/handled
                                #(rf/dispatch [::add-strip
                                               strips-context default/charge-group-strip]))}
            [:i.fas.fa-plus] " " [tr :string.button/add]]]

          [:div.option.charge-group-strips
           (into [:ul]
                 (map (fn [idx]
                        (let [strip-context (c/++ strips-context idx)]
                          ^{:key idx}
                          [:li
                           [:div.no-select {:style {:padding-right "10px"
                                                    :white-space "nowrap"}}
                            [:a (if (zero? idx)
                                  {:class "disabled"}
                                  {:on-click (js-event/handled
                                              #(rf/dispatch [::component.element/move
                                                             strip-context
                                                             (c/++ strips-context (dec idx))
                                                             {:no-select? true}]))})
                             [:i.fas.fa-chevron-up]]
                            " "
                            [:a (if (= idx (dec num-strips))
                                  {:class "disabled"}
                                  {:on-click (js-event/handled
                                              #(rf/dispatch [::component.element/move
                                                             strip-context
                                                             (c/++ strips-context (+ idx 2))
                                                             {:no-select? true}]))})
                             [:i.fas.fa-chevron-down]]]
                           [:div
                            [strip-form strip-context type-str]]
                           [:div {:style {:padding-left "10px"}}
                            [:a (if (< num-strips 2)
                                  {:class "disabled"}
                                  {:on-click (js-event/handled
                                              #(rf/dispatch [::component.element/remove strip-context]))})
                             [:i.far.fa-trash-alt]]]])))
                 (range num-strips))]]))

     [element/element (c/++ context :manual-blazon)]]))

(defmethod component/node :heraldry/charge-group [context]
  (let [charges-context (c/++ context :charges)
        num-charges (interface/get-list-size charges-context)]
    {:title (string/str-tr :string.charge-group/charge-group-of " " (if (= num-charges 1)
                                                                      (charge.options/title (c/++ context :charges 0))
                                                                      :string.charge-group/various))
     :icon {:default (static/static-url
                      (str "/svg/charge-group-preset-three.svg"))
            :selected (static/static-url
                       (str "/svg/charge-group-preset-three-selected.svg"))}
     :draggable? true
     :drop-options-fn drag/drop-options
     :drop-fn drag/drop-fn
     :buttons [{:icon "fas fa-plus"
                :title :string.button/add
                :menu [{:title :string.entity/charge
                        :handler #(rf/dispatch [::component.element/add charges-context default/charge])}]}
               {:icon "far fa-clone"
                :title :string.button/duplicate
                :handler #(rf/dispatch [::component.element/duplicate context])}]
     :nodes (concat (->> (range num-charges)
                         (map (fn [idx]
                                (let [charge-context (c/++ charges-context idx)]
                                  {:context charge-context
                                   :buttons [{:icon "far fa-trash-alt"
                                              :title :string.tooltip/remove
                                              :remove? true
                                              :handler #(rf/dispatch [::component.element/remove
                                                                      charge-context
                                                                      remove-charge-options])}]})))
                         vec))}))

(defmethod component/form :heraldry/charge-group [_context]
  form)
