(ns heraldicon.frontend.ui.blazonry-editor.parser
  (:require
   ["genex" :as genex]
   [clojure.string :as s]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.ui.blazonry-editor.shared :as shared]
   [heraldicon.reader.blazonry.parser :as parser]
   [heraldicon.reader.blazonry.reader :as reader]
   [re-frame.core :as rf]))

(def ^:private parser-path
  (conj shared/blazonry-editor-path :parser))

(def ^:private status-path
  (conj shared/blazonry-editor-path :status))

(rf/reg-event-db ::update
  (fn [db [_ charges]]
    (cond-> db
      (-> db
          (get-in (conj parser-path :charges))
          (not= charges)) (assoc-in parser-path {:charges charges
                                                 :parser (parser/generate charges)}))))

(defn parse-blazonry [blazon parser]
  (try
    (let [hdn (reader/read blazon parser)]
      {:blazon blazon
       :hdn hdn})
    (catch :default e
      (let [{:keys [reason
                    index]} (ex-data e)
            suggestions (->> reason
                             (mapcat (fn [{:keys [tag expecting]}]
                                       (case tag
                                         :optional []
                                         :string [expecting]
                                         :regexp (-> expecting
                                                     genex
                                                     .generate
                                                     js->clj))))
                             (map s/trim)
                             (remove s/blank?)
                             sort
                             dedupe
                             (map (fn [choice]
                                    [choice (-> parser
                                                :suggestion-classifications
                                                (get choice))])))]
        {:blazon blazon
         :error (when (not reason)
                  (ex-message e))
         :suggestions suggestions
         :index index}))))

(rf/reg-event-db ::clear-status
  (fn [db _]
    (assoc-in db status-path nil)))

(defn- set-status [db hdn error]
  (assoc-in db status-path
            {:status (if hdn
                       :success
                       (when error
                         :error))
             :error error
             :warnings (->> hdn
                            (tree-seq (some-fn vector? map? seq?) seq)
                            (keep (fn [data]
                                    (when (map? data)
                                      (let [warnings (concat
                                                      (:heraldicon.reader.blazonry.transform/warnings data)
                                                      (:heraldicon.reader.blazonry.process/warnings data))]
                                        (when (seq warnings)
                                          warnings)))))
                            (apply concat))}))

(defn status []
  (let [{:keys [status error warnings]} @(rf/subscribe [:get status-path])]
    [:<>
     (case status
       :success [:span.parser-success [tr :string.blazonry-editor/success]]
       :error [:span.parser-error [tr :string.blazonry-editor/error] ": " error]
       nil)
     (when (seq warnings)
       (into [:ul.parser-warnings]
             (map (fn [warning]
                    [:li [tr :string.blazonry-editor/warning] ": " warning]))
             warnings))]))

(rf/reg-event-fx ::parse
  (fn [{:keys [db]} [_ text]]
    (let [parser (get-in db (conj parser-path :parser) parser/default)
          {:keys [hdn error] :as result} (parse-blazonry text parser)]
      {:db (set-status db hdn error)
       :dispatch [:heraldicon.frontend.ui.blazonry-editor.editor/on-parse-result text result]})))
