(ns heraldry.frontend.ui.form.ribbon-general
  (:require
   [heraldry.context :as c]
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.macros :as macros]
   [heraldry.frontend.ui.element.select :as select]
   [heraldry.frontend.ui.element.submenu :as submenu]
   [heraldry.frontend.ui.element.text-field :as text-field]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.gettext :refer [string]]
   [heraldry.interface :as interface]
   [heraldry.math.curve :as curve]
   [heraldry.ribbon :as ribbon]
   [heraldry.util :as util]
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

    :waves (let [mod4 (mod n 4)]
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
                                                                    :waves} flow-mode :stacked) (->> (- even-max-num-curves)))]
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

(defn segment-form [context]
  (let [segment-type (interface/get-raw-data (c/++ context :type))
        idx (-> context :path last)
        z-index (interface/get-sanitized-data (c/++ context :z-index))
        title (util/str-tr (inc idx) ". "
                           (ribbon/segment-type-map segment-type)
                           ", layer " z-index)]

    [:div {:style {:position "relative"}}
     [submenu/submenu context nil [tr title] {:style {:width "28em"}
                                              :class "submenu-segment-form"}
      (ui-interface/form-elements
       context
       [:type
        :z-index
        :font
        :font-scale
        :spacing
        :offset-x
        :offset-y])]

     [text-field/text-field (c/++ context :text)
      :style {:display "inline-block"
              :position "absolute"
              :left "13em"}]]))

(defn ribbon-form [context]
  (ui-interface/form-elements
   context
   [:thickness
    :edge-angle
    :end-split
    :outline?]))

(defn ribbon-segments-form [context & {:keys [tooltip]}]
  (let [num-segments (interface/get-list-size (c/++ context :segments))]
    [:div.option.ribbon-segments {:style {:margin-top "0.5em"}}
     [:div {:style {:font-size "1.3em"}} [tr (string "Segments")]
      (when tooltip
        [:div.tooltip.info {:style {:display "inline-block"
                                    :margin-left "0.2em"
                                    :vertical-align "top"}}
         [:i.fas.fa-question-circle]
         [:div.bottom {:style {:width "20em"}}
          [tr tooltip]]])]

     [:ul
      (doall
       (for [idx (range num-segments)]
         ^{:key idx}
         [segment-form (c/++ context :segments idx)]))]

     [:p {:style {:color "#f86"}}
      [tr (string "The SVG export embeds the fonts, but some programs might not display them correctly. At least Chrome should display it.")]]]))

(defn form [context]
  [:<>
   (ui-interface/form-elements
    context
    [:name
     :attribution
     :is-public
     :attributes
     :tags])

   [ribbon-form (c/++ context :ribbon)]

   [:div {:style {:font-size "1.3em"
                  :margin-top "0.5em"
                  :margin-bottom "0.5em"}} [tr (string "Topology")]
    [:div.tooltip.info {:style {:display "inline-block"
                                :margin-left "0.2em"
                                :vertical-align "top"}}
     [:i.fas.fa-question-circle]
     [:div.bottom {:style {:width "20em"}}
      [:p [tr (string "The ribbon curve can be interpreted in many ways, depending on what is fore-/background and which segments overlap which.")]]
      [:p [tr (string "The Presets can be used to setup the segments for some typical effects, the segments can then be fine-tuned.")]]]]]

   [:p {:style {:color "#f86"}}
    [tr (string "Apply a preset after you edited the ribbon curve and changed the number of segments.")]]

   (let [layer-mode-value (or @(rf/subscribe [:get layers-path])
                              layer-mode-default)
         flow-mode-value (or @(rf/subscribe [:get flow-path])
                             flow-mode-default)
         start-mode-value (or @(rf/subscribe [:get start-path])
                              start-mode-default)]
     [:<>
      [select/raw-select
       {:path layers-path}
       layer-mode-value
       (string "Layering presets")
       [[(string "Middle outwards") :middle-outwards]
        [(string "Left to right") :left-to-right]
        [(string "Right to left") :right-to-left]]]

      [select/raw-select
       {:path flow-path}
       flow-mode-value
       (string "Flow presets")
       [[(string "Stacked") :stacked]
        [(string "Spiral clockwise") :spiral-clockwise]
        [(string "Spiral counter-clockwise") :spiral-counter-clockwise]
        [(string "Waves") :waves]]]

      [select/raw-select
       {:path start-path}
       start-mode-value
       (string "Start presets")
       [[(string "Foreground") :foreground]
        [(string "Background") :background]]]

      [:div
       [:button {:on-click #(rf/dispatch [:ribbon-edit-annotate-segments
                                          (-> context :path (conj :ribbon))
                                          layer-mode-value
                                          flow-mode-value
                                          start-mode-value])}
        [tr (string "Apply presets")]]
       [:button {:on-click #(rf/dispatch [:ribbon-edit-invert-segments
                                          (-> context :path (conj :ribbon))])}
        [tr (string "Invert")]]]

      [ribbon-segments-form
       (c/++ context :ribbon)
       :tooltip (string "Segments can be background, foreground, or foreground with text and their rendering order is determined by the layer number.\n\nNote: apply a preset after introducing new segments or removing segments in the curve. This will overwrite changes here, but right now there's no good way to preserve this.")]])])

(defmethod ui-interface/component-node-data :heraldry.component/ribbon-general [context]
  {:title (string "General")
   :validation @(rf/subscribe [:validate-ribbon-general context])})

(defmethod ui-interface/component-form-data :heraldry.component/ribbon-general [_context]
  {:form form})
