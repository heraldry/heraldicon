(ns heraldry.frontend.ui.form.ribbon-general
  (:require [heraldry.frontend.ui.element.select :as select]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.element.text-field :as text-field]
            [heraldry.frontend.ui.interface :as ui-interface]
            [heraldry.frontend.macros :as macros]
            [heraldry.math.curve :as curve]
            [heraldry.ribbon :as ribbon]
            [re-frame.core :as rf]))

(def layers-path
  [:ui :ribbon-presets :layers])

(def flow-path
  [:ui :ribbon-presets :flow])

(def start-path
  [:ui :ribbon-presets :start])

(def layer-mode-default
  :middle-outwards)

(def flow-mode-default
  :stacked)

(def start-mode-default
  :foreground)

(defn flow-fn [mode n]
  (case mode
    :stacked n

    :spiral-clockwise (let [mod2 (mod n 2)]
                        (cond
                          (= mod2 1) (+ n 1)
                          (and (zero? mod2)
                               (not (zero? n))) (- n 1)
                          :else n))

    :spiral-counter-clockwise (let [mod2 (mod n 2)]
                                (cond
                                  (= mod2 1) (- n 1)
                                  (zero? mod2) (+ n 1)))

    :nebuly (let [mod4 (mod n 4)]
              (cond
                (= mod4 2) (+ n 2)
                (and (zero? mod4)
                     (not (zero? n))) (- n 2)
                :else n))))

(defn type-fn [mode n segment-length]
  (if (case mode
        :foreground (even? n)
        :background (odd? n))
    (if (>= segment-length 0.1)
      :heraldry.ribbon.segment/foreground-with-text
      :heraldry.ribbon.segment/foreground)
    :heraldry.ribbon.segment/background))

(defn restore-previous-text-segments [segments previous-segments keys]
  (let [previous-text-segments (->> previous-segments
                                    (keep (fn [segment]
                                            (when (-> segment
                                                      :type
                                                      (= :heraldry.ribbon.segment/foreground-with-text))
                                              (select-keys segment keys))))
                                    vec)
        text-segments (->> segments
                           (map :type)
                           (keep-indexed
                            (fn [idx segment-type]
                              (when (= segment-type :heraldry.ribbon.segment/foreground-with-text)
                                idx)))
                           vec)]
    (loop [segments segments
           [previous-text-idx & rest] (range (count previous-text-segments))]
      (if (or (not previous-text-idx)
              (>= previous-text-idx (count text-segments)))
        segments
        (recur (update segments (get text-segments previous-text-idx)
                       merge (get previous-text-segments previous-text-idx))
               rest)))))

(macros/reg-event-db :ribbon-edit-annotate-segments
  (fn [db [_ path layer-mode flow-mode start-mode]]
    (let [segments-path (conj path :segments)
          points (get-in db (conj path :points))
          starts-right? (> (-> points first :x)
                           (-> points last :x))
          layer-mode (cond
                       (and (= layer-mode :left-to-right)
                            starts-right?) :right-to-left
                       (and (= layer-mode :right-to-left)
                            starts-right?) :left-to-right
                       :else layer-mode)
          ;; TODO: duplicating the default value for :edge-angle here, not using options
          edge-angle (or (get-in db (conj path :edge-angle))
                         0)
          {:keys [curves curve]} (ribbon/generate-curves points edge-angle)
          num-curves (count curves)
          even-max-num-curves (if (even? num-curves)
                                num-curves
                                (inc num-curves))
          total-length (curve/length curve)]
      (-> db
          (assoc-in
           segments-path
           (case layer-mode
             :left-to-right (vec (map-indexed
                                  (fn [idx curve]
                                    {:type (type-fn start-mode idx
                                                    (/ (curve/length curve)
                                                       total-length))
                                     :index idx
                                     :z-index (flow-fn flow-mode idx)}) curves))

             :right-to-left (vec (map-indexed
                                  (fn [idx curve]
                                    (let [reverse-idx (-> num-curves
                                                          dec
                                                          (- idx))]
                                      {:type (type-fn start-mode reverse-idx
                                                      (/ (curve/length curve)
                                                         total-length))
                                       :index idx
                                       :z-index (flow-fn flow-mode reverse-idx)})) curves))

             :middle-outwards (vec (map-indexed
                                    (fn [idx curve]
                                      (let [effective-type-idx (-> num-curves
                                                                   (quot 2)
                                                                   (- idx)
                                                                   Math/abs)
                                            effective-flow-idx (cond-> effective-type-idx
                                                                 (#{:stacked
                                                                    :nebuly} flow-mode :stacked) (->> (- even-max-num-curves)))]
                                        {:type (type-fn start-mode effective-type-idx
                                                        (/ (curve/length curve)
                                                           total-length))
                                         :index idx
                                         :z-index (flow-fn flow-mode effective-flow-idx)})) curves))
             []))
          (update-in segments-path
                     (fn [segments]
                       (let [new-segments (->> segments
                                               (map (fn [segment]
                                                      (if
                                                       (-> segment :type
                                                           (= :heraldry.ribbon.segment/foreground-with-text))
                                                        (assoc segment :text "LOREM IPSUM")
                                                        (dissoc segment :text))))
                                               vec)]
                         (restore-previous-text-segments
                          new-segments
                          (get-in db segments-path)
                          [:offset-x
                           :offset-y
                           :font-scale
                           :spacing
                           :text
                           :font]))))))))

(macros/reg-event-db :ribbon-edit-invert-segments
  (fn [db [_ path]]
    (let [segments-path (conj path :segments)]

      (update-in
       db segments-path
       (fn [segments]
         (let [max-z-index (->> segments
                                (map :z-index)
                                (apply max))]
           (->> segments
                (map (fn [segment]
                       (-> segment
                           (update :type {:heraldry.ribbon.segment/foreground :heraldry.ribbon.segment/background
                                          :heraldry.ribbon.segment/foreground-with-text :heraldry.ribbon.segment/background
                                          :heraldry.ribbon.segment/background :heraldry.ribbon.segment/foreground})
                           (update :z-index #(- max-z-index %)))))
                vec)))))))

(defn segment-form [path type-str]
  (let [segment-type @(rf/subscribe [:get-value (conj path :type)])
        idx (last path)
        z-index @(rf/subscribe [:get-sanitized-data (conj path :z-index)])
        title (str (inc idx) ". "
                   (case segment-type
                     :heraldry.ribbon.segment/foreground-with-text "Text"
                     :heraldry.ribbon.segment/foreground "Foreground"
                     :heraldry.ribbon.segment/background "Background")
                   ", layer " z-index)]

    [:div {:style {:position "relative"}}
     [submenu/submenu path type-str title {:style {:width "28em"}
                                           :class "submenu-segment-form"}
      (for [option [:type
                    :z-index
                    :font
                    :font-scale
                    :spacing
                    :offset-x
                    :offset-y]]
        ^{:key option} [ui-interface/form-element (conj path option)])]
     [text-field/text-field (conj path :text)
      :style {:display "inline-block"
              :position "absolute"
              :left "13em"}]]))

(defn ribbon-form [path]
  [:<>
   (for [option [:thickness
                 :edge-angle
                 :end-split
                 :outline?]]
     ^{:key option} [ui-interface/form-element (conj path option)])])

(defn ribbon-segments-form [path & {:keys [tooltip]}]
  (let [segments-path (conj path :segments)
        num-segments @(rf/subscribe [:get-list-size segments-path])]
    [:div.option.ribbon-segments {:style {:margin-top "0.5em"}}
     [:div {:style {:font-size "1.3em"}} "Segments"
      (when tooltip
        [:div.tooltip.info {:style {:display "inline-block"
                                    :margin-left "0.2em"
                                    :vertical-align "top"}}
         [:i.fas.fa-question-circle]
         [:div.bottom {:style {:width "20em"}}
          tooltip]])]

     [:ul
      (doall
       (for [idx (range num-segments)]
         (let [segment-path (conj segments-path idx)]
           ^{:key idx}
           [segment-form segment-path])))]

     [:p {:style {:color "#f86"}}
      "The SVG export embeds the fonts, but some programs might not display them correctly. At least Chrome should display it."]]))

(defn form [path _]
  [:<>
   (for [option [:name
                 :attribution
                 :is-public
                 :attributes
                 :tags]]
     ^{:key option} [ui-interface/form-element (conj path option)])

   [ribbon-form (conj path :ribbon)]

   [:div {:style {:font-size "1.3em"
                  :margin-top "0.5em"
                  :margin-bottom "0.5em"}} "Topology"
    [:div.tooltip.info {:style {:display "inline-block"
                                :margin-left "0.2em"
                                :vertical-align "top"}}
     [:i.fas.fa-question-circle]
     [:div.bottom {:style {:width "20em"}}
      [:p "The ribbon curve can be interpreted in many ways, depending on what is fore-/background and which segments overlap which."]
      [:p "The Presets can be used to setup the segments for some typical effects, the segments can then be fine-tuned."]]]]

   [:p {:style {:color "#f86"}}
    "Apply a preset after you edited the ribbon curve and changed the number of segments."]

   (let [layer-mode-value (or @(rf/subscribe [:get-value layers-path])
                              layer-mode-default)
         flow-mode-value (or @(rf/subscribe [:get-value flow-path])
                             flow-mode-default)
         start-mode-value (or @(rf/subscribe [:get-value start-path])
                              start-mode-default)]
     [:<>
      [select/raw-select
       layers-path
       layer-mode-value
       "Layering presets"
       [["Middle outwards" :middle-outwards]
        ["Left to right" :left-to-right]
        ["Right to left" :right-to-left]]]

      [select/raw-select
       flow-path
       flow-mode-value
       "Flow presets"
       [["Stacked" :stacked]
        ["Spiral clockwise" :spiral-clockwise]
        ["Spiral counter-clockwise" :spiral-counter-clockwise]
        ["Nebuly" :nebuly]]]

      [select/raw-select
       start-path
       start-mode-value
       "Start presets"
       [["Foreground" :foreground]
        ["Background" :background]]]

      [:div
       [:button {:on-click #(rf/dispatch [:ribbon-edit-annotate-segments
                                          (conj path :ribbon)
                                          layer-mode-value
                                          flow-mode-value
                                          start-mode-value])}
        "Apply presets"]
       [:button {:on-click #(rf/dispatch [:ribbon-edit-invert-segments
                                          (conj path :ribbon)])}
        "Invert"]]

      [ribbon-segments-form
       (conj path :ribbon)
       :tooltip [:<>
                 [:p "Segments can be background, foreground, or foreground with text and their rendering order is determined by the layer number."]
                 [:p "Note: apply a preset after introducing new segments or removing segments in the curve. This will overwrite changes here, but right now there's no good way to preserve this."]]]])])

(defmethod ui-interface/component-node-data :heraldry.component/ribbon-general [path]
  {:title "General"
   :validation @(rf/subscribe [:validate-ribbon-general path])})

(defmethod ui-interface/component-form-data :heraldry.component/ribbon-general [_path]
  {:form form})
