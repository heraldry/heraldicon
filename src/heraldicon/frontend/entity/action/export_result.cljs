(ns heraldicon.frontend.entity.action.export-result
  (:require
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.modal :as modal]
   [heraldicon.util.sanitize :as sanitize]))

(defn filename [entity-type name suffix extension]
  (str (sanitize/filename name (case entity-type
                                 :heraldicon.entity.type/arms "arms"
                                 :heraldicon.entity.type/collection "collection"
                                 "export"))
       suffix
       "." extension))

(defn show [url filename]
  (modal/create
   :string.export/ready-title
   [:div {:style {:padding "0.5em"}}
    [:p [tr :string.export/ready-message]]
    [:p [:a {:href url
             :download filename
             :target "_blank"
             :rel "noopener"
             :on-click (fn [_] (modal/clear))}
         [:i.fas.fa-download {:style {:margin-right "0.5em"}}]
         filename]]]))
