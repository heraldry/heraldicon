(ns heraldry.frontend.ui.form.ribbon-general
  (:require [heraldry.frontend.ui.element.select :as select]
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.interface :as ui-interface]
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

(defn type-fn [mode n]
  (if (case mode
        :foreground (even? n)
        :background (odd? n))
    :heraldry.ribbon.segment/foreground
    :heraldry.ribbon.segment/background))

(rf/reg-event-db :ribbon-edit-annotate-segments
  (fn [db [_ path layer-mode flow-mode start-mode]]
    (let [segments-path (conj path :segments)
          points (get-in db (conj path :points))
          {:keys [curves]} (ribbon/generate-curves points)
          num-curves (count curves)
          even-max-num-curves (if (even? num-curves)
                                num-curves
                                (inc num-curves))]
      (assoc-in
       db segments-path
       (case layer-mode
         :back-to-front (vec (map-indexed
                              (fn [idx _curve]
                                {:type (type-fn start-mode idx)
                                 :index idx
                                 :z-index (flow-fn flow-mode idx)}) curves))
         :front-to-back (vec (map-indexed
                              (fn [idx _curve]
                                (let [reverse-idx (-> num-curves
                                                      dec
                                                      (- idx))]
                                  {:type (type-fn start-mode reverse-idx)
                                   :index idx
                                   :z-index (flow-fn flow-mode reverse-idx)})) curves))
         :middle-outwards (vec (map-indexed
                                (fn [idx _curve]
                                  (let [effective-type-idx (-> num-curves
                                                               (quot 2)
                                                               (- idx)
                                                               Math/abs)
                                        effective-flow-idx (cond-> effective-type-idx
                                                             (#{:stacked
                                                                :nebuly} flow-mode :stacked) (->> (- even-max-num-curves)))]
                                    {:type (type-fn start-mode effective-type-idx)
                                     :index idx
                                     :z-index (flow-fn flow-mode effective-flow-idx)})) curves))
         [])))))

(rf/reg-event-db :ribbon-edit-invert-segments
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
                                          :heraldry.ribbon.segment/background :heraldry.ribbon.segment/foreground})
                           (update :z-index #(- max-z-index %)))))
                vec)))))))

(defn segment-form [path type-str]
  (let [segment-type @(rf/subscribe [:get-sanitized-data (conj path :type)])
        idx (last path)
        z-index @(rf/subscribe [:get-sanitized-data (conj path :z-index)])
        title (str (inc idx) ". "
                   (case segment-type
                     :heraldry.ribbon.segment/foreground "Foreground"
                     :heraldry.ribbon.segment/background "Background")
                   ", layer " z-index)]

    [:div {:style {:position "relative"}}
     [submenu/submenu path type-str title {:style {:width "23em"}
                                           :class "submenu-segment-form"}
      (for [option [:type
                    :z-index]]
        ^{:key option} [ui-interface/form-element (conj path option)])]]))

(defn form [path _]
  [:<>
   (for [option [:name
                 :attribution
                 :is-public
                 :attributes
                 :tags]]
     ^{:key option} [ui-interface/form-element (conj path option)])

   (for [option [:thickness]]
     ^{:key option} [ui-interface/form-element (conj path :ribbon option)])

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

   (let [layer-mode-value (or @(rf/subscribe [:get-value layers-path])
                              layer-mode-default)
         flow-mode-value (or @(rf/subscribe [:get-value flow-path])
                             flow-mode-default)
         start-mode-value (or @(rf/subscribe [:get-value start-path])
                              start-mode-default)
         segments-path (conj path :ribbon :segments)
         num-segments @(rf/subscribe [:get-list-size segments-path])]
     [:<>
      [select/raw-select
       layers-path
       layer-mode-value
       "Layering presets"
       [["Middle outwards" :middle-outwards]
        ["Back to front" :back-to-front]
        ["Front to back" :front-to-back]]]

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

      [:div.option.ribbon-segments {:style {:margin-top "0.5em"}}
       [:div {:style {:font-size "1.3em"}} "Segments"
        [:div.tooltip.info {:style {:display "inline-block"
                                    :margin-left "0.2em"
                                    :vertical-align "top"}}
         [:i.fas.fa-question-circle]
         [:div.bottom {:style {:width "20em"}}
          [:p "Segments can be background, foreground, or foreground with text and their rendering order is determined by the layer number."]
          [:p "Note: apply a preset after introducing new segments or removing segments in the curve. This will overwrite changes here, but right now there's no good way to preserve this."]]]]
       [:ul
        (doall
         (for [idx (range num-segments)]
           (let [segment-path (conj segments-path idx)]
             ^{:key idx}
             [segment-form segment-path])))]]])])

(defmethod ui-interface/component-node-data :heraldry.component/ribbon-general [path]
  {:title "General"
   :validation @(rf/subscribe [:validate-ribbon-general path])})

(defmethod ui-interface/component-form-data :heraldry.component/ribbon-general [_path]
  {:form form})

