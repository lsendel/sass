from flask import Flask, request, jsonify
from src.agents.constitutional_enforcement import ConstitutionalEnforcementAgent
from src.agents.tdd_compliance import TDDComplianceAgent

app = Flask(__name__)

@app.route('/api/constitutional/validate', methods=['POST'])
def validate_constitutional_compliance():
    """API endpoint for validating constitutional compliance."""
    feature_data = request.json
    
    agent = ConstitutionalEnforcementAgent()
    validation_result = agent.validate_constitutional_compliance(feature_data)
    
    return jsonify({
        "compliant": validation_result["compliant"],
        "violations": validation_result["violations"],
        "enforcement_actions": validation_result["required_actions"],
        "authority": "SUPREME"
    })

@app.route('/api/tdd/validate', methods=['POST'])
def validate_tdd_compliance():
    """API endpoint for validating TDD compliance."""
    files_data = request.json
    test_files = files_data.get("test_files", [])
    implementation_files = files_data.get("implementation_files", [])
    
    agent = TDDComplianceAgent()
    validation_result = agent.validate_tdd_compliance(test_files, implementation_files)
    
    return jsonify({
        "tdd_compliant": validation_result["compliant"],
        "phase": validation_result["current_phase"],
        "violations": validation_result["violations"],
        "required_actions": validation_result["required_actions"]
    })

if __name__ == '__main__':
    app.run(host='localhost', port=8080)