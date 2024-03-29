(ns heraldicon.frontend.component.entity.ribbon.data
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.component.core :as component]
   [heraldicon.frontend.component.ribbon :as form.ribbon]
   [heraldicon.frontend.element.select :as select]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.tooltip :as tooltip]
   [heraldicon.frontend.validation :as validation]
   [heraldicon.heraldry.ribbon :as ribbon]
   [heraldicon.math.curve.core :as curve]
   [re-frame.core :as rf]))

(def ^:private layers-path
  [:ui :ribbon-presets :layers])

(def ^:private flow-path
  [:ui :ribbon-presets :flow])

(def ^:private start-path
  [:ui :ribbon-presets :start])

(def ^:private layer-mode-default
  :middle-outwards)

(def ^:private flow-mode-default
  :stacked)

(def ^:private start-mode-default
  :foreground)

(defn- flow-fn [mode n]
  (case mode
    :stacked n

    :spiral-clockwise (let [mod2 (mod n 2)]
                        (cond
                          (= mod2 1) (inc n)
                          (and (zero? mod2)
                               (not (zero? n))) (dec n)
                          :else n))

    :spiral-counter-clockwise (let [mod2 (mod n 2)]
                                (cond
                                  (= mod2 1) (dec n)
                                  (zero? mod2) (inc n)))

    :waves (let [mod4 (mod n 4)]
             (cond
               (= mod4 2) (+ n 2)
               (and (zero? mod4)
                    (not (zero? n))) (- n 2)
               :else n))))

(defn- type-fn [mode n segment-length]
  (if (case mode
        :foreground (even? n)
        :background (odd? n))
    (if (>= segment-length 0.1)
      :heraldry.ribbon.segment.type/foreground-with-text
      :heraldry.ribbon.segment.type/foreground)
    :heraldry.ribbon.segment.type/background))

(defn restore-previous-text-segments [segments previous-segments keys]
  (let [previous-text-segments (->> previous-segments
                                    (keep (fn [segment]
                                            (when (-> segment
                                                      :type
                                                      (= :heraldry.ribbon.segment.type/foreground-with-text))
                                              (select-keys segment keys))))
                                    vec)
        text-segments (->> segments
                           (map :type)
                           (keep-indexed
                            (fn [idx segment-type]
                              (when (= segment-type :heraldry.ribbon.segment.type/foreground-with-text)
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

(macros/reg-event-db ::annotate-segments
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
                                                      (if (-> segment :type
                                                              (= :heraldry.ribbon.segment.type/foreground-with-text))
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

(macros/reg-event-db ::invert-segments
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
                           (update :type {:heraldry.ribbon.segment.type/foreground :heraldry.ribbon.segment.type/background
                                          :heraldry.ribbon.segment.type/foreground-with-text :heraldry.ribbon.segment.type/background
                                          :heraldry.ribbon.segment.type/background :heraldry.ribbon.segment.type/foreground})
                           (update :z-index #(- max-z-index %)))))
                vec)))))))

(defn- form [context]
  [:<>
   [form.ribbon/form (c/++ context :ribbon)]

   [:div {:style {:font-size "1.3em"
                  :margin-top "0.5em"
                  :margin-bottom "0.5em"}} [tr :string.ribbon/topology]
    [tooltip/info
     [:<>
      [:p [tr :string.ribbon.text/topology-explanation-1]]
      [:p [tr :string.ribbon.text/topology-explanation-2]]]
     :width "20em"]]

   [:p {:style {:color "#f86"}}
    [tr :string.ribbon.text/apply-preset-after-change]]

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
       :string.ribbon/layering-presets
       [[:string.ribbon.layering-presets-choice/middle-outwards :middle-outwards]
        [:string.ribbon.layering-presets-choice/left-to-right :left-to-right]
        [:string.ribbon.layering-presets-choice/right-to-left :right-to-left]]]

      [select/raw-select
       {:path flow-path}
       flow-mode-value
       :string.ribbon/flow-presets
       [[:string.ribbon.flow-presets-choice/stacked :stacked]
        [:string.ribbon.flow-presets-choice/spiral-clockwise :spiral-clockwise]
        [:string.ribbon.flow-presets-choice/spiral-counter-clockwise :spiral-counter-clockwise]
        [:string.ribbon.flow-presets-choice/waves :waves]]]

      [select/raw-select
       {:path start-path}
       start-mode-value
       :string.ribbon/start-presets
       [[:string.ribbon.start-presets-choice/foreground :foreground]
        [:string.ribbon.start-presets-choice/background :background]]]

      [:div
       [:button {:on-click #(rf/dispatch [::annotate-segments
                                          (-> context :path (conj :ribbon))
                                          layer-mode-value
                                          flow-mode-value
                                          start-mode-value])}
        [tr :string.ribbon/apply-presets]]
       [:button {:on-click #(rf/dispatch [::invert-segments
                                          (-> context :path (conj :ribbon))])}
        [tr :string.ribbon.button/invert]]]

      [form.ribbon/segments-form
       (c/++ context :ribbon)
       :tooltip :string.ribbon.text/segment-explanation]])])

(defmethod component/node :heraldicon.entity.ribbon/data [context]
  {:title :string.miscellaneous/general
   :validation (validation/validate-entity context)})

(defmethod component/form :heraldicon.entity.ribbon/data [_context]
  form)
