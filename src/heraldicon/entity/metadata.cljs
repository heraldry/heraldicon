(ns heraldicon.entity.metadata)

(def known-metadata-keys
  ["source"
   "source url"
   "blazon"
   "armiger"
   "house"
   "location name"
   "location latitude"
   "location longitude"
   "year exact"
   "year approximate"
   "year min"
   "year max"])

(defn options [_context]
  {:ui/label :string.entity/metadata
   :ui/element :ui.element/metadata})
