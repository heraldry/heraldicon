(ns heraldicon.frontend.ui.element.blazonry-editor.parser
  (:require
   ["genex" :as genex]
   [clojure.string :as s]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.ui.element.blazonry-editor.shared :as shared]
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

(defn- caret-position [index]
  (let [selection (js/document.getSelection)
        range-count (.-rangeCount selection)]
    (when (pos? range-count)
      (let [range (.getRangeAt selection 0)
            node (.-startContainer range)
            node-length (-> node .-length (or 0))
            offset (if index
                     (min index node-length)
                     (.-startOffset range))]
        (cond
          (pos? offset) (let [rect (.getBoundingClientRect
                                    (doto (js/document.createRange)
                                      (.setStart node (dec offset))
                                      (.setEnd node offset)))]
                          {:top (.-top rect)
                           :left (.-left rect)})
          (< offset
             node-length) (let [rect (.getBoundingClientRect
                                      (doto (js/document.createRange)
                                        (.setStart node offset)
                                        (.setEnd node (inc offset))))]
                            {:top (.-top rect)
                             :left (.-left rect)})
          :else (let [node (first (js/document.getElementsByClassName "DraftEditor-root"))
                      rect (.getBoundingClientRect node)
                      styles (js/getComputedStyle node)
                      line-height (js/parseInt (.-lineHeight styles))
                      font-size (js/parseInt (.-fontSize styles))
                      delta (/ (- line-height font-size) 2)]
                  {:top (-> rect
                            .-top
                            (+ delta))
                   :left (.-left rect)}))))))

(def ^:private suggestion-hint-order
  (into {}
        (map-indexed (fn [index hint]
                       [hint index]))
        ["layout"
         "cottising"
         "line"
         "fimbriation"
         "extra tincture"
         "tincture"
         "ordinary"
         "ordinary option"
         "charge"
         "charge option"
         "partition"
         "attitude"
         "facing"
         "number"]))

(defn- parse-blazonry [blazon cursor-index parser & {:keys [api?]}]
  (try
    (let [hdn (reader/read blazon parser)]
      {:blazon blazon
       :hdn hdn})
    (catch :default e
      (let [{:keys [reason
                    index]} (ex-data e)
            cursor-index (-> cursor-index
                             (max index)
                             (min (count blazon)))
            typed-string (s/trim (subs blazon index cursor-index))
            auto-complete-choices (->> reason
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
                                       (filter (fn [choice]
                                                 (or api?
                                                     (if choice
                                                       (s/starts-with? choice typed-string)
                                                       true))))
                                       sort
                                       dedupe
                                       (map (fn [choice]
                                              [choice (-> parser
                                                          :suggestion-classifications
                                                          (get choice))]))
                                       (sort-by (fn [[choice hint]]
                                                  [(get suggestion-hint-order hint 1000)
                                                   choice]))
                                       vec)
            position (when-not api?
                       (caret-position index))]
        {:blazon blazon
         :error (when (not reason)
                  (ex-message e))
         :auto-complete (cond-> {:choices auto-complete-choices}
                          (not api?) (assoc :on-click
                                            (fn [choice]
                                              (rf/dispatch [::auto-completion-clicked index cursor-index choice])))
                          position (assoc :position position))
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
  (fn [{:keys [db]} [_ text cursor-index]]
    (let [parser (get-in db (conj parser-path :parser) parser/default)
          {:keys [hdn error] :as result} (parse-blazonry text cursor-index parser)]
      {:db (set-status db hdn error)
       :dispatch [:heraldicon.frontend.ui.element.blazonry-editor.editor/on-parse-result text result]})))
