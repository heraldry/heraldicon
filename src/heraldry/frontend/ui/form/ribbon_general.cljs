(ns heraldry.frontend.ui.form.ribbon-general
  (:require [heraldry.frontend.ui.element.select :as select]
            [heraldry.frontend.ui.interface :as ui-interface]
            [heraldry.ribbon :as ribbon]
            [re-frame.core :as rf]))

(rf/reg-event-db :ribbon-edit-annotate-segments
  (fn [db [_ path mode]]
    (let [segments-path (conj path :segments)
          points (get-in db (conj path :points))
          {:keys [curves]} (ribbon/generate-curves points)]
      (assoc-in
       db segments-path
       (case mode
         :back-to-front (vec (map-indexed
                              (fn [idx _curve]
                                {:type (if (even? idx)
                                         :heraldry.ribbon.segment/foreground
                                         :heraldry.ribbon.segment/background)
                                 :index idx
                                 :z-index idx}) curves))
         :front-to-back (vec (map-indexed
                              (fn [idx _curve]
                                (let [reverse-idx (-> curves
                                                      count
                                                      dec
                                                      (- idx))]
                                  {:type (if (even? reverse-idx)
                                           :heraldry.ribbon.segment/foreground
                                           :heraldry.ribbon.segment/background)
                                   :index idx
                                   :z-index reverse-idx})) curves))
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

   [:<>
    [select/raw-select
     nil
     :none
     "Presets"
     [["--- Reset ---" :none]
      ["Back to front" :back-to-front]
      ["Front to back" :front-to-back]
      ["Center to back" :center-to-ends]
      ["Alternating" :alternating]]
     :on-change #(rf/dispatch [:ribbon-edit-annotate-segments (conj path :ribbon) %])]]])

(defmethod ui-interface/component-node-data :heraldry.component/ribbon-general [path]
  {:title "General"
   :validation @(rf/subscribe [:validate-ribbon-general path])})

(defmethod ui-interface/component-form-data :heraldry.component/ribbon-general [_path]
  {:form form})

