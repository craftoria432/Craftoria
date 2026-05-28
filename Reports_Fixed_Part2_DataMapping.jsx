// PART 2: CRITICAL FIX - Data Mapping Function
// Replace the loadReports function in your Reports.jsx with this:

const loadReports = useCallback(async () => {
  try {
    setLoading(true);
    const snapshot = await getDocs(query(collection(db, 'reports')));
    
    if (snapshot.docs.length > 0) {
      const data = snapshot.docs.map(d => {
        const docData = d.data();
        
        // ✅ FIX: Correctly map Firebase data to app schema
        return {
          id: d.id,
          type: getTypeLabel(docData.type || 'product'),
          typeKey: docData.type || 'product',
          icon: docData.type || 'product',
          
          // ✅ FIX: Correctly extract reporter information from Firebase
          reporter: {
            id: docData.reporter_id || '',
            name: docData.reporter_name || 'Unknown User',
            avatar: (docData.reporter_name || 'U').substring(0, 2).toUpperCase()
          },
          
          // ✅ FIX: Correctly extract reported entity information from Firebase
          reportedEntity: {
            id: docData.reported_entity_id || '',
            name: docData.reported_entity_name || 'Unknown Entity'
          },
          
          reason: docData.reason || 'No reason provided',
          description: docData.description || 'No description provided',
          status: docData.status || 'New',
          date: docData.created_at 
            ? new Date(docData.created_at).toLocaleDateString('en-US', { 
                year: 'numeric', 
                month: 'long', 
                day: 'numeric' 
              })
            : new Date().toLocaleDateString(),
          evidence: docData.evidence || [],
          source: docData.source || 'mobile app'
        };
      });
      
      setReports(data);
      setFilteredReports(data);
    } else {
      setReports(sampleReports);
      setFilteredReports(sampleReports);
    }
  } catch (error) {
    console.error('Error loading reports:', error);
    setReports(sampleReports);
    setFilteredReports(sampleReports);
  } finally {
    setLoading(false);
  }
}, []);

// Helper function to get readable type labels
const getTypeLabel = (type) => {
  const labels = {
    'product': 'Inappropriate Products',
    'seller': 'Seller Misconduct',
    'buyer': 'Buyer Complaints',
    'technical': 'Technical Issues'
  };
  return labels[type] || 'Inappropriate Products';
};
