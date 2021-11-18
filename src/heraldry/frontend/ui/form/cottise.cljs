(ns heraldry.frontend.ui.form.cottise
  (:require
   [heraldry.context :as c]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.interface :as interface]
   [re-frame.core :as rf]))

(defn form [context]
  (ui-interface/form-elements
   context
   [:line
    :opposite-line
    :distance
    :thickness
    :outline?]))

;; TODO: context
(defn cottise-name [{:keys [path] :as context}]
  (let [cottise-key (last path)
        ordinary-type (interface/get-raw-data (-> context
                                                  (c/-- 2)
                                                  (c/++ :type)))]
    (-> (cond
          (#{:heraldry.ordinary.type/pale}
           ordinary-type) {:cottise-1 "1 left"
                           :cottise-2 "2 left"
                           :cottise-opposite-1 "1 right"
                           :cottise-opposite-2 "2 right"}
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

(defmethod ui-interface/component-node-data :heraldry.component/cottise [context]
  {:title (str "Cottise " (cottise-name context))
   :validation @(rf/subscribe [:validate-cottise context])
   :nodes [{:context (c/++ context :field)}]})

(defmethod ui-interface/component-form-data :heraldry.component/cottise [_context]
  {:form form})
