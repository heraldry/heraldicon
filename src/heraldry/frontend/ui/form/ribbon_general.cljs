(ns heraldry.frontend.ui.form.ribbon-general
  (:require [heraldry.frontend.ui.element.select :as select]
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
  :back-to-front)

(def flow-mode-default
  :stacked)

(def start-mode-default
  :foreground)

(defn flow-fn [mode n]
  (case mode
    :stacked n

    :alternating (let [mod2 (mod n 2)]
                   (cond
                     (= mod2 1) (+ n 1)
                     (and (zero? mod2)
                          (not (zero? n))) (- n 1)
                     :else n))

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
          {:keys [curves]} (ribbon/generate-curves points)]
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
                                (let [reverse-idx (-> curves
                                                      count
                                                      dec
                                                      (- idx))]
                                  {:type (type-fn start-mode reverse-idx)
                                   :index idx
                                   :z-index (flow-fn flow-mode reverse-idx)})) curves))
         [])))))

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
       [["Back to front" :back-to-front]
        ["Front to back" :front-to-back]
        ["Center to back" :center-to-ends]]]

      [select/raw-select
       flow-path
       flow-mode-value
       "Flow presets"
       [["Stacked" :stacked]
        ["Alternating" :alternating]
        ["Nebuly" :nebuly]]]

      [select/raw-select
       start-path
       start-mode-value
       "Start presets"
       [["Foreground" :foreground]
        ["Background" :background]]]

      [:button {:on-click #(rf/dispatch [:ribbon-edit-annotate-segments
                                         (conj path :ribbon)
                                         layer-mode-value
                                         flow-mode-value
                                         start-mode-value])}
       "Apply presets"]])])

(defmethod ui-interface/component-node-data :heraldry.component/ribbon-general [path]
  {:title "General"
   :validation @(rf/subscribe [:validate-ribbon-general path])})

(defmethod ui-interface/component-form-data :heraldry.component/ribbon-general [_path]
  {:form form})

