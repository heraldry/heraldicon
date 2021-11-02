(ns heraldry.frontend.ui.form.cottise
  (:require
   [heraldry.context :as c]
   [heraldry.frontend.ui.interface :as ui-interface]
   [re-frame.core :as rf]))

(defn form [context]
  [:<>
   (for [option [:line
                 :opposite-line
                 :distance
                 :thickness
                 :outline?]]
     ^{:key option} [ui-interface/form-element (c/++ context option)])])

;; TODO: context
(defn cottise-name [path]
  (let [cottise-key (last path)
        ordinary-type @(rf/subscribe [:get-value (-> (drop-last 2 path)
                                                     vec
                                                     (conj :type))])]
    (-> (cond
          (#{:heraldry.ordinary.type/pale}
           ordinary-type) {:cottise-1 "1 right"
                           :cottise-2 "2 right"
                           :cottise-opposite-1 "1 left"
                           :cottise-opposite-2 "2 left"}
          (#{:heraldry.ordinary.type/fess
             :heraldry.ordinary.type/bend
             :heraldry.ordinary.type/bend-sinister
             :heraldry.ordinary.type/chevron}
           ordinary-type) {:cottise-1 "1 top"
                           :cottise-2 "2 top"
                           :cottise-opposite-1 "1 bottom"
                           :cottise-opposite-2 "2 bottom"}
          :else {:cottise-1 "1"
                 :cottise-2 "2"
                 :cottise-opposite-1 "1 (opposite)"
                 :cottise-opposite-2 "2 (opposite)"
                 :cottise-extra-1 "1 (extra)"
                 :cottise-extra-2 "2 (extra)"})
        (get cottise-key))))

(defmethod ui-interface/component-node-data :heraldry.component/cottise [{:keys [path] :as context}]
  {:title (str "Cottise " (cottise-name path))
   :validation @(rf/subscribe [:validate-cottise path])
   :nodes [{:context (c/++ context :field)}]})

(defmethod ui-interface/component-form-data :heraldry.component/cottise [_context]
  {:form form})
